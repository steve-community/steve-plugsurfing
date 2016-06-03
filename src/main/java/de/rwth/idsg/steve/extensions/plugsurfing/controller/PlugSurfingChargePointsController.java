package de.rwth.idsg.steve.extensions.plugsurfing.controller;

import de.rwth.idsg.steve.extensions.plugsurfing.dto.Contact;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.StationForm;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.StationRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingService;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.utils.ControllerHelper;
import de.rwth.idsg.steve.web.controller.ChargePointsController;
import de.rwth.idsg.steve.web.dto.ChargePointBatchInsertForm;
import jooq.steve.db.tables.records.PsChargeboxRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 07.01.2016
 */
@Controller
public class PlugSurfingChargePointsController extends ChargePointsController {

    @Autowired private StationRepository stationRepository;
    @Autowired private PlugSurfingService plugSurfingService;

    @RequestMapping(value = DETAILS_PATH, method = RequestMethod.GET)
    public String getDetails(@PathVariable("chargeBoxPk") int chargeBoxPk, Model model) {
        ChargePoint.Details cp = chargePointRepository.getDetails(chargeBoxPk);

        StationForm form = new StationForm();
        form.setChargeBoxPk(cp.getChargeBox().getChargeBoxPk());
        form.setChargeBoxId(cp.getChargeBox().getChargeBoxId());
        form.setNote(cp.getChargeBox().getNote());
        form.setDescription(cp.getChargeBox().getDescription());
        form.setLocationLatitude(cp.getChargeBox().getLocationLatitude());
        form.setLocationLongitude(cp.getChargeBox().getLocationLongitude());

        form.setAddress(ControllerHelper.recordToDto(cp.getAddress()));

        PsChargeboxRecord stationRecord = stationRepository.getPlugSurfingStationRecord(chargeBoxPk);
        if(stationRecord != null) {
            setPlugSurfingUI(form, stationRecord);
        }
        model.addAttribute("chargePointForm", form);
        model.addAttribute("cp", cp);
        addCountryCodes(model);

        return "data-man/chargepointDetails";
    }

    @RequestMapping(value = ADD_PATH, method = RequestMethod.GET)
    public String addGet(Model model) {
        model.addAttribute("chargePointForm", new StationForm());
        model.addAttribute("batchChargePointForm", new ChargePointBatchInsertForm());
        addCountryCodes(model);
        return "data-man/chargepointAdd";
    }

    @RequestMapping(params = "add", value = ADD_SINGLE_PATH, method = RequestMethod.POST)
    public String addSinglePost(@Valid @ModelAttribute("chargePointForm") StationForm chargePointForm,
                                BindingResult result, Model model) {
        if (result.hasErrors()) {
            addCountryCodes(model);
            model.addAttribute("batchChargePointForm", new ChargePointBatchInsertForm());
            return "data-man/chargepointAdd";
        }

        int pk = chargePointRepository.addChargePoint(chargePointForm);

        if (chargePointForm.getPlugSurfing()) {
            chargePointForm.setChargeBoxPk(pk);
            stationRepository.updateOrAddPlugSurfingStation(chargePointForm);
        }
        return toOverview();
    }

    @RequestMapping(value = ADD_BATCH_PATH, method = RequestMethod.POST)
    public String addBatchPost(@Valid @ModelAttribute("batchChargePointForm") ChargePointBatchInsertForm form,
                               BindingResult result, Model model) {
        if (result.hasErrors()) {
            addCountryCodes(model);
            model.addAttribute("chargePointForm", new StationForm());
            return "data-man/chargepointAdd";
        }

        chargePointRepository.addChargePoint(form.getIdList());
        return toOverview();
    }

    @RequestMapping(params = "update", value = UPDATE_PATH, method = RequestMethod.POST)
    public String update(@Valid @ModelAttribute("chargePointForm") StationForm chargePointForm,
                        BindingResult result, Model model) {
        if (result.hasErrors()) {
            addCountryCodes(model);
            return "data-man/chargepointDetails";
        }

        chargePointRepository.updateChargePoint(chargePointForm);

        if (chargePointForm.getPlugSurfing()) {
            stationRepository.updateOrAddPlugSurfingStation(chargePointForm);
            plugSurfingService.postCompleteStationDataWithStatus(chargePointForm.getChargeBoxId());
        } else {
            //Could be that the user updates a PS Station, this means that it's unchecked
            //We need to discard all the details about the additional PS information
            plugSurfingService.postConnectorStatusOffline(chargePointForm.getChargeBoxPk());
            stationRepository.disablePlugSurfingStation(chargePointForm.getChargeBoxPk());
        }
        return toOverview();
    }

    @RequestMapping(value = DELETE_PATH, method = RequestMethod.POST)
    public String delete(@PathVariable("chargeBoxPk") int chargeBoxPk) {
        plugSurfingService.postConnectorStatusOffline(chargeBoxPk);
        chargePointRepository.deleteChargePoint(chargeBoxPk);
        return toOverview();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void setPlugSurfingUI(StationForm form, PsChargeboxRecord station) {
        form.setPlugSurfing(station.getIsEnabled());
        form.setOpen24(station.getIsOpen_24());
        form.setReservable(station.getIsReservable());
        form.setFloorLevel(station.getFloorLevel());
        form.setFreeCharge(station.getIsFreeCharge());
        form.setTotalParking(station.getTotalParking());
        form.setGreenPowerAvailable(station.getIsGreenPowerAvailable());
        form.setPluginCharge(station.getIsPluginCharge());
        form.setRoofed(station.getIsRoofed());
        form.setPrivatelyOwned(station.getIsPrivate());
        form.setNumberOfConnectors(station.getNumberOfConnectors());

        Contact contact = new Contact();
        contact.setPhone(station.getPhone());
        contact.setFax(station.getFax());
        contact.setWebsite(station.getWebsite());
        contact.setEmail(station.getEmail());
        form.setContact(contact);
    }
}
