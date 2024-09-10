package com.esrx.plb.rxb.impl;

import com.esrx.plb.commons.model.process.*;
import com.esrx.plb.commons.process.PlbIntentFunctions;
import com.esrx.plb.commons.utils.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;
import org.rxbxml.model.BPLHeader;
import org.rxbxml.model.PharmacyBenefits;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.esrx.plb.commons.utils.ApplicationConstants.*;
import static com.esrx.plb.rxb.util.RxbConstants.*;

@Slf4j
public class RxbIntentObject extends PlbIntentObject implements PlbIntentFunctions {


    @Override
    public void parseIntentXml() {
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
                            createPlbSetup(cbmEntity,homeDeliveryProgram,rxbPlbSetups,++setupIdx);
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
     *
     * cbmEntity - CBM entity
     * homeDeliveryProgram (HDP)
     * */
    private void createPlbSetup(String cbmEntity, String homeDeliveryProgram, List<PlbSetup> rxbPlbSetups,short idx) {
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
                    if (getLogic(ce.getOtherInfo(),PLAN_FLAG).equalsIgnoreCase(M_LOGIC)) {
                        plbProducts.add(createPlbProduct(PLAN_FLAG,M_LOGIC));
                    }

                    //When Copay flag is present - create Copay product
                    if (getLogic(ce.getOtherInfo(),COPAY_FLAG).equalsIgnoreCase(M_LOGIC)) {
                        plbProducts.add(createPlbProduct(COPAY_FLAG,M_LOGIC));
                    }
                }
                plbSetup.setPlbProducts(plbProducts);
            }
        }
        rxbPlbSetups.add(plbSetup);
    }

    private List<PharmacyEntity> createPharmacyEntities(List<String> networks) {
        List<PharmacyEntity> pharmacyEntities = new ArrayList<>();
        for (String network: networks) {
            if (network.trim().toUpperCase().equalsIgnoreCase(ALL)) {
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
            product  = new PlbProduct();
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
            product  = new PlbProduct();
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
                String [] networks = info.split(":");
                if (networks.length > 0) {
                    String [] netList = networks[1].split("~");
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
                String [] flag = info.split(":");
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
    public void customIntentFunctions() {
        log.info("Building Rxb report data");
    }

    @Override
    public void buildPlbSetups() {
        log.info("Building PLB Setups for RxB");
        // TODO
        //  build the PlbSetup list based on the JSON and XML data

        List<PlbSetup> rxbPlbSetups = new ArrayList<>();

    }
}
