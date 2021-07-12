package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_UIN_UPDATE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import io.mosip.registration.controller.GenericController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

/**
 * UpdateUINController Class.
 * 
 * @author Mahesh Kumar
 *
 */
@Controller
public class UpdateUINController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(UpdateUINController.class);

	@Autowired
	private RegistrationController registrationController;

	@FXML
	private TextField uinId;

	@FXML
	private Button backBtn;
	@FXML
	private ImageView backImageView;
	@FXML
	private ImageView continueImageView;

	@Autowired
	private UinValidator<String> uinValidatorImpl;

	@Autowired
	Validations validation;

	@Autowired
	private GenericController genericController;

	@FXML
	FlowPane parentFlowPane;

	private ObservableList<Node> parentFlow;

	private HashMap<String, Object> checkBoxKeeper;

	private Map<String, List<UiSchemaDTO>> groupedMap;

	private FXUtils fxUtils;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

//		backImageView	images/arrowLeft.png
//		continueImageView	images/arrowRight.png

		setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
		setImage(continueImageView, RegistrationConstants.ARROW_RIGHT_IMG);
		
		fxUtils = FXUtils.getInstance();
		checkBoxKeeper = new HashMap<>();

		SchemaDto schema = getLatestSchema();

		groupedMap = schema.getSchema().stream().filter(field -> field.getGroup() != null && field.isInputRequired())
				.collect(Collectors.groupingBy(UiSchemaDTO::getGroup));

		parentFlow = parentFlowPane.getChildren();
		groupedMap.forEach((groupName, list) -> {
			UiSchemaDTO groupField = schema.getSchema().stream().filter(field -> field.getGroup() != null && field.getGroup().equalsIgnoreCase(groupName)).findFirst().get();
			GridPane checkBox = addCheckBox(groupName, groupField);
			if (checkBox != null) {
				parentFlow.add(checkBox);
			}
		});

		try {
			
			backBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
				if (newValue) {
					setImage(backImageView, RegistrationConstants.BACK_FOCUSED_IMG);
				} else {
					setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
				}
			});
		} catch (Throwable runtimeException) {
			LOGGER.error(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	private GridPane addCheckBox(String groupName, UiSchemaDTO field) {
		String groupLabel = getLabelsForGroup(groupName, field);
		CheckBox checkBox = new CheckBox(groupLabel);
		checkBox.setId(groupName);
		checkBox.setTooltip(new Tooltip(groupLabel));
		checkBox.getStyleClass().add(RegistrationConstants.updateUinCheckBox);
		fxUtils.listenOnSelectedCheckBox(checkBox);
		checkBoxKeeper.put(groupName, checkBox);
		
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(400);
		gridPane.setPrefHeight(40);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(85);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);

		ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
		RowConstraints rowConstraint1 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		RowConstraints rowConstraint2 = new RowConstraints();
		columnConstraint1.setPercentWidth(60);
		RowConstraints rowConstraint3 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		rowConstraints.addAll(rowConstraint1, rowConstraint2, rowConstraint3);

		gridPane.add(checkBox, 1, 1);

		return gridPane;
	}

	private String getLabelsForGroup(String groupName, UiSchemaDTO field) {
		String groupLabel = RegistrationConstants.EMPTY;
		if (field.getGroupLabel() != null && !field.getGroupLabel().isEmpty()) {
			for (String langCode : registrationController.getSelectedLangList()) {
				if (field.getGroupLabel().containsKey(langCode)) {
					groupLabel = groupLabel.isBlank() ? field.getGroupLabel().get(langCode) : groupLabel.concat(RegistrationConstants.SLASH).concat(field.getGroupLabel().get(langCode));
				}
			}
		}
		return groupLabel.isBlank() ? groupName : groupLabel;
	}

	/**
	 * Submitting for UIN update after selecting the required fields.
	 *
	 * @param event the event
	 */
	@FXML
	public void submitUINUpdate(ActionEvent event) {
		LOGGER.info(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID, "Updating UIN details");
		try {
			if (StringUtils.isEmpty(uinId.getText())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPDATE_UIN_ENTER_UIN_ALERT));
			} else {
				Map<String, UiSchemaDTO> selectedFields = new HashMap<String, UiSchemaDTO>();
				List<String> selectedFieldGroups = new ArrayList<String>();
				for (String key : checkBoxKeeper.keySet()) {
					if (((CheckBox) checkBoxKeeper.get(key)).isSelected()) {
						selectedFieldGroups.add(key);
						for (UiSchemaDTO field : groupedMap.get(key)) {
							selectedFields.put(field.getId(), field);
						}
					}
				}

				LOGGER.debug("selectedFieldGroups : {} , selectedFields : {}",selectedFieldGroups, selectedFields);

				if(selectedFields.isEmpty()) {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPDATE_UIN_SELECTION_ALERT));
					return;
				}

				if (uinValidatorImpl.validateId(uinId.getText()) && !selectedFields.isEmpty()) {
					registrationController.init(uinId.getText(), checkBoxKeeper, selectedFields, selectedFieldGroups);
					Parent createRoot = BaseController.load(
							getClass().getResource(RegistrationConstants.CREATE_PACKET_PAGE),
							applicationContext.getBundle(getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0), RegistrationConstants.LABELS));

					getScene(createRoot).setRoot(createRoot);
					genericController.populateScreens();
					return;
				}
			}
		} catch (InvalidIDException invalidIdException) {
			LOGGER.error(invalidIdException.getMessage(), invalidIdException);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPDATE_UIN_VALIDATION_ALERT));
		} catch (Throwable exception) {
			LOGGER.error(exception.getMessage(), exception);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
		}
		clearRegistrationData();
	}
}
