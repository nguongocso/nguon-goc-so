package vn.nguongocso.farm.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Projection interface for FarmLog entity.
 * This interface defines the structure of the data that will be retrieved from the database for farm logs.
 */
public interface FarmLogProjection {
	UUID getId();

	UUID getProductionLotId();

	String getProductionLotName();

	FarmActivityType getActivityType();

	String getMaterial();

	Double getQuantity();

	String getUnit();

	LocalDate getExecutedDate();

	String getNotes();

	String getCreatedByName();

	LocalDateTime getCreatedAt();
}
