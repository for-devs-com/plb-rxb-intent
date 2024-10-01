package com.esrx.plb.rxb.impl;

import com.esrx.plb.commons.dto.hc13.request.*;
import com.esrx.plb.commons.dto.hc13.request.PharmacyNetworkDetails;
import com.esrx.plb.commons.dto.hc13.request.ProductDetails;
import com.esrx.plb.commons.dto.hc13.response.HC13Response;
import com.esrx.plb.commons.model.process.*;
import com.esrx.plb.commons.process.PlbIntentFunctions;
import com.esrx.plb.commons.utils.ApplicationConstants;
import com.esrx.plb.commons.utils.ProcessFailedException;
import com.esrx.plb.commons.utils.PropertyProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rxbxml.model.BPLHeader;
import org.rxbxml.model.PharmacyBenefits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.esrx.plb.commons.utils.ApplicationConstants.*;
import static com.esrx.plb.rxb.util.RxbConstants.*;

@Slf4j
@Getter
@Setter
@Service
public class RxbIntentObject extends PlbIntentObject implements PlbIntentFunctions {

    @Autowired
    PropertyProvider propertyProvider;

    /*@Autowired
    private PlbRuleService plbRuleService;*/

    @Override
    public void parseIntentXml() throws ProcessFailedException {
        log.info("Parsing Rxb Xml");
        //TODO this method should parse the XML and build the plbSetups
        List<PlbSetup> rxbPlbSetups = new ArrayList<>();
        try {
            if (getIntentXml() != null) {
                JAXBContext jaxbContext = JAXBContext.newInstance(PharmacyBenefits.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                StringReader stringReader = new StringReader(getIntentXml());
                PharmacyBenefits pharmacyBenefits = (PharmacyBenefits) unmarshaller.unmarshal(stringReader);
                if (pharmacyBenefits.getBPLs() != null && pharmacyBenefits.getBPLs().getBPLHeader().size() > 0) {
                    List<BPLHeader> bplHeaders = pharmacyBenefits.getBPLs().getBPLHeader();
                    short setupIdx = 0;
                    for (BPLHeader bplHeader : bplHeaders) {
                        String homeDeliveryProgram = bplHeader.getHomeDeliveryProgram();
                        String cbmEntity = bplHeader.getBPLID();
                        setXmlParseError(!Arrays.stream(HOME_DELIVERY_PROGRAM_VALID_VALUES).anyMatch(s -> s.equals(homeDeliveryProgram)));
                        if (!isXmlParseError()) {
                            createPlbSetup(cbmEntity, homeDeliveryProgram, rxbPlbSetups, ++setupIdx);
                        } else {
                            //TODO Should throw ProcessFailedException
                            // is this a fatal error?

                            setXmlParseErrorMessage("Invalid HomeDeliveryProgram");

                        }
                    }
                }
            }
            //Rxb Setups
            this.setPlbSetups(rxbPlbSetups);
        } catch (Exception ex) {

        }
    }

    /**
     * RxB logic to create PLB rule setups
     * <p>
     * cbmEntity - CBM entity
     * homeDeliveryProgram (HDP)
     */
    private void createPlbSetup(String cbmEntity, String homeDeliveryProgram, List<PlbSetup> rxbPlbSetups, short idx) {
        //For each CBM Entity in the JSON
        //find the associated setup from the Intent XML
        PlbSetup plbSetup = new PlbSetup(idx);
        for (CbmEntity ce : getCbmEntities()) {
            if (ce.getName().equalsIgnoreCase(cbmEntity)) {
                //pharmacy entity list for this setup
                plbSetup.setPharmacyEntities(createPharmacyEntities(getNetworks(ce.getOtherInfo())));
                List<PlbProduct> plbProducts = new ArrayList<>();
                plbSetup.setCbmEntity(ce.getName());

                if (Arrays.stream(new String[]{"4", "5", "6", "7", "8", "9", "10", "11"}).anyMatch(s -> s.equals(homeDeliveryProgram))) {
                    //HDP = 8,9,10,11 and copay flag = M logic create a setup with
                    // channel = retail
                    // pharmacy = ALL
                    // appliesTo = Maintenance
                    plbSetup.setChannel(ApplicationConstants.PLB_CHANNEL_TYPE_RETAIL);
                    plbSetup.setHierarchyType(ApplicationConstants.PLB_MAINTENANCE);
                    //When Plan flag is present - create Plan product
                    if (getLogic(ce.getOtherInfo(), PLAN_FLAG).equalsIgnoreCase(M_LOGIC)) {
                        plbProducts.add(createPlbProduct(PLAN_FLAG, M_LOGIC));
                    }

                    //When Copay flag is present - create Copay product
                    if (getLogic(ce.getOtherInfo(), COPAY_FLAG).equalsIgnoreCase(M_LOGIC)) {
                        plbProducts.add(createPlbProduct(COPAY_FLAG, M_LOGIC));
                    }
                }
                plbSetup.setPlbProducts(plbProducts);
            }
        }
        rxbPlbSetups.add(plbSetup);
    }

    private List<PharmacyEntity> createPharmacyEntities(List<String> networks) {
        List<PharmacyEntity> pharmacyEntities = new ArrayList<>();
        for (String network : networks) {
            if (network.trim().equalsIgnoreCase(ALL)) {
                //Create All Pharmacy and break the loop
                PharmacyEntity allPharmacy = new PharmacyEntity();
                allPharmacy.setPharmacyId(ALL);
                allPharmacy.setPharmacyType(PHARMACY_ALL);
                allPharmacy.setPharmacyName(ALL);
                allPharmacy.setValidPharmacy(true);
                pharmacyEntities.add(allPharmacy);
                break;
            } else {
                //Add all pharmacies
                PharmacyEntity pharmacy = new PharmacyEntity();
                pharmacy.setPharmacyId(network.trim().toUpperCase());
                //We would only receive Network entities for RxB
                pharmacy.setPharmacyType(PHARMACY_NETWORK);
                pharmacyEntities.add(pharmacy);
            }
        }
        return pharmacyEntities;
    }

    //TODO may need to update this method in the future for creating Products with other attributes
    // Can this method be a candidate for moving to commons?
    private PlbProduct createPlbProduct(String prdFlag, String attrVal) {
        PlbProduct product = null;
        if (prdFlag.equalsIgnoreCase(COPAY_FLAG)) {
            //Create Copay product
            product = new PlbProduct();
            product.setProductType(PRODUCT_COPAY);
            product.setValidProduct(true);
            product.setAction("A");
            List<ProductAttribute> prdAttrs = new ArrayList<>();
            ProductAttribute attr = new ProductAttribute();
            attr.setName(ATTR_COPAY_INDICATOR);
            attr.setValue(M_LOGIC);
            prdAttrs.add(attr);
            product.setProductAttributes(prdAttrs);

        } else if (prdFlag.equalsIgnoreCase(PLAN_FLAG)) {
            //Create Plan product
            product = new PlbProduct();
            product.setProductType(PRODUCT_PLAN);
            product.setValidProduct(true);
            product.setAction("A");
            List<ProductAttribute> prdAttrs = new ArrayList<>();
            ProductAttribute attr = new ProductAttribute();
            attr.setName(ATTR_PLAN_INDICATOR);
            attr.setValue(M_LOGIC);
            prdAttrs.add(attr);
            product.setProductAttributes(prdAttrs);
        }
        return product;
    }

    private List<String> getNetworks(String otherInfo) {
        List<String> rtn = null;
        String[] otherInfoArray = otherInfo.split(",");
        for (String info : otherInfoArray) {
            if (info.trim().toUpperCase().startsWith("NETWORK")) {
                String[] networks = info.split(":");
                if (networks.length > 0) {
                    String[] netList = networks[1].split("~");
                    rtn = Arrays.asList(netList);
                }
            }
        }

        return rtn;
    }

    private String getLogic(String otherInfo, String prdFlag) {
        String rtn = null;
        String[] otherInfoArray = otherInfo.split(",");
        for (String info : otherInfoArray) {
            if (info.trim().toUpperCase().startsWith(prdFlag)) {
                String[] flag = info.split(":");
                if (flag.length > 0) {
                    if (flag[1].trim().toUpperCase().startsWith(Y_LOGIC)) rtn = Y_LOGIC;
                    else if (flag[1].trim().toUpperCase().startsWith(M_LOGIC)) rtn = M_LOGIC;
                }
            }
        }
        return rtn;
    }


    @Override
    public void buildIntentObject() {
        log.info("Building Rxb Intent object");
    }

    @Override
    public void sendIntentToMainframe() {
        log.info("Sending Rxb intent to mainframe");
    }

    @Override
    public void buildReportData() {
        log.info("Building Rxb report data");
    }

    @Override
    public void setJsonMessage(String jsonMessage) {
        log.info("Received Rxb intent");
        super.setJsonMessage(jsonMessage);
    }

    @Override
    public void compareIntentWithCurrentRules() throws ProcessFailedException {
        //TODO call HC13
        // compare with the intent rules
        // the result of this comparison should create PLB rules for MF call
        try {
            FindRequest hc13Request = new FindRequest();
            hc13Request.setCountOnly(YES_Y);
            //TODO would have to call once for each CBM entity
            hc13Request.setCarrierAgnId(String.valueOf(this.getCbmEntities().get(0).getAgn()));

            ClientEntityDetails clientEntityDetails = new ClientEntityDetails();
            List<ClientEntity> clientEntities = new ArrayList<>();
            ClientEntity clientEntity = new ClientEntity();
           List<CbmEntity> cbmEntities =  getCbmEntities();
           if(cbmEntities.size() > 0) {
               CbmEntity cbmentity = cbmEntities.get(0);
               hc13Request.setAsOfDate(cbmentity.getChangeEffDate()); // entity_change_eff_date from Json of entity_info[0]

               clientEntity.setEntityAgnId(String.valueOf(cbmentity.getAgn())); // entity_agn from entity_info [0]
               clientEntity.setEntityName(String.valueOf(cbmentity.getName())); // entity_name from entity_info [0]
           }
            clientEntities.add(clientEntity);
            clientEntityDetails.setClientEntities(clientEntities);

            PharmacyNetworkDetails pharmacyNetworkDetails = new PharmacyNetworkDetails();
            List<PharmacyNetworkEntity> pharmacyNetworkEntities = new ArrayList<>();
            PharmacyNetworkEntity pharmacyNetworkEntity = new PharmacyNetworkEntity();
            pharmacyNetworkEntity.setAllPharmacyInd(NO_IND); // always N, get from Constants
            pharmacyNetworkEntities.add(pharmacyNetworkEntity);
            pharmacyNetworkDetails.setPharmacyNetworkEntities(pharmacyNetworkEntities);

            ProductDetails productDetails = new ProductDetails();
            productDetails.setChannelType("B");
            Copay copay = new Copay();
            copay.setIndicator(NONE); // search for all copays, it should always be "NONE"
            productDetails.setCopay(copay);
            productDetails.setHierarchyIndicartor("2");
            hc13Request.setTestMode(NO_IND); // always N
            hc13Request.setUserId(propertyProvider.ldapUser); // user_id from config server (ldap.username)
            //hc13Request.setOrgId(ORG_ID);  // ESI always move into constants
            hc13Request.setChannel(PLB); // always PLB
            hc13Request.setTimeOut(Long.valueOf(propertyProvider.searchBenefitTimeOut)); // move this into Config properties
            //hc13Request.setTimeOut(30000L);
            hc13Request.setCarrierName(this.getCarrierDiv()); // 'carrier_div' from Request Json
            hc13Request.setCarrierAgnId("33368691");
            //hc13Request.setCarrierAgnId(String.valueOf(plbRuleService.fetchCarrierOrDivAgn(this.getCarrierDiv()))); // fetch "fetchCarrierOrDivAgn() from plb-postgres API
            hc13Request.setStartingRuleIndex(RULE_INDEX); // move into constants
            hc13Request.setEndingRuleIndex(MAX_HC13_RULE_INDEX);
            //hc13Request.setAsOfDate(this.getEffectiveDateOfChange()); // entity_change_eff_date from Json of entity_info[0]
            hc13Request.setRecordStatus(RECORD_STATUS); // move into constants
            //hc13Request.setCountOnly(NO_IND);  // move into constants and alwasys N
            hc13Request.setClientEntityDetails(clientEntityDetails);
            hc13Request.setPharmacyNetworkDetails(pharmacyNetworkDetails);
            hc13Request.setProductDetails(productDetails);

            HC13Response hc13Response = getHc13Service().findBenefitRules(hc13Request); // get count
            log.info("HC13 Response :  {}", hc13Response.getStatusCode());

            int ruleCount = Integer.parseInt(hc13Response.getRuleCount().getClientsPLBCount().get(0).getClientRuleCount());
            List<HC13Response.PLBRuleDtls> existingRulesDetails = new ArrayList<>();
            if(ruleCount > 70){
                int startCount = ruleCount;
                int startIndex = 1;
                while(startCount > 0) {
                    hc13Request.setCountOnly(NO_N);
                    hc13Request.setStartingRuleIndex(String.valueOf(startIndex));
                    int endIndex= startIndex+69;
                    hc13Request.setEndingRuleIndex(String.valueOf(endIndex));
                    HC13Response hc13ResponseObj = getHc13Service().findBenefitRules(hc13Request);
                    // set existingRules to a base object
                    if(hc13ResponseObj != null && hc13ResponseObj.getBenefitsRule() != null) {
                        if(this.getExistingRules() == null){
                            existingRules = hc13ResponseObj.getBenefitsRule().getPLBRuleDtls();
                        } else {
                            this.getExistingRules().addAll(hc13ResponseObj.getBenefitsRule().getPLBRuleDtls());
                        }
                    }
                    startIndex = endIndex+1;
                    startCount= startCount-70;
                }
            } else {
                hc13Request.setCountOnly(YES_Y);
                hc13Request.setStartingRuleIndex(String.valueOf(1));
                hc13Request.setEndingRuleIndex(String.valueOf(ruleCount));
                HC13Response hc13ResponseObj = getHc13Service().findBenefitRules(hc13Request);
                // set existingRules to a base object
                existingRules = hc13ResponseObj.getBenefitsRule().getPLBRuleDtls();
            }
            //TODO loop through for each 70 rules
            // hc13Response.getRuleCount() >70 set hc13Request.setStartingRuleIndex(); and endingindex()
            // hc13Response.getBenefitsRule().getPLBRuleDtls() should set to the base object (existingRules)
            // getHc13Service().findBenefitRules(new FindRequest()); // get rules
        } catch (Exception e) {
            throw new ProcessFailedException(ProcessFailedException.ErrorTypes.PROCESSING_ERROR, e.getMessage());
        }
    }


    @Override
    public void buildPlbIntentRules() {
        log.info("Building PLB Intent Rules for RxB");
        // TODO
        //  build the PlbRules from the intent using the JSON and XML data

        List<PlbRule> plbRules = new ArrayList<>();

        //Iterate through each PLB setup
        for (PlbSetup setup : this.getPlbSetups()) {
            //Iterate through each CBM entity
            for (CbmEntity entity : this.getCbmEntities()) {
                //If the setup is for the CBM entity
                if(setup.getCbmEntity() !=null && !setup.getCbmEntity().isEmpty()) {
                    if (setup.getCbmEntity().equalsIgnoreCase(entity.getName())) {
                        //For each PLB Product
                        for (PlbProduct product : setup.getPlbProducts()) {
                            //For each Pharmacy
                            for (PharmacyEntity pharmacy : setup.getPharmacyEntities()) {
                                PlbRule plbRule = new PlbRule();
                                plbRule.setEffDateOfChange(this.getIntentEffDate());
                                plbRule.setOperation(PLB_RULE_ADD);
                                plbRule.setParentAgn(String.valueOf(this.getParentAgn()));
                                plbRule.setClientAgn(String.valueOf(entity.getAgn()));
                                //For RxB there are no client exclusions
                                plbRule.setEntityExclInd(NO_N);
                                //plbRule.setClientExclAgns();

                                PharmacyDetails pharmacyDetails = new PharmacyDetails();
                                pharmacyDetails.setPharmacyId(pharmacy.getPharmacyId());
                                pharmacyDetails.setPharmacyTypeCode(String.valueOf(pharmacy.getPharmacyType()));

                                plbRule.setPharmacyDetails(pharmacyDetails);
                                plbRule.setDomainType(DOMAIN_TYPE_1); //set to 1
                                plbRule.setProductType(product.getProductType());
                                plbRule.setProductHierarchy(setup.getHierarchyType());

                                plbRule.setProductAttributes(product.getProductAttributes());
                                plbRule.setEndDate(RULE_ENDDATE);
                                plbRule.setChannel(setup.getChannel());

                                plbRules.add(plbRule);
                            }
                        }
                    }
                }
            }
        }
        this.setIntentRules(plbRules);
    }
}
