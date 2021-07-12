package io.mosip.registration.service;


import java.util.List;

import io.mosip.registration.dto.schema.SettingsSchema;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.exception.RegBaseCheckedException;

public interface IdentitySchemaService {
	
	public Double getLatestEffectiveSchemaVersion() throws RegBaseCheckedException;
		
	public List<UiSchemaDTO> getLatestEffectiveUISchema() throws RegBaseCheckedException;
	
	public String getLatestEffectiveIDSchema() throws RegBaseCheckedException;
	
	public List<UiSchemaDTO> getUISchema(double idVersion) throws RegBaseCheckedException;
	
	public String getIDSchema(double idVersion) throws RegBaseCheckedException;
	
	public SchemaDto getIdentitySchema(double idVersion) throws RegBaseCheckedException;

	public List<SettingsSchema> getSettingsSchema(double idVersion) throws RegBaseCheckedException;

}
