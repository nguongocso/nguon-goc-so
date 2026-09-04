package vn.nguongocso.event.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.event.service.impl.ProcurementEventServiceImpl;
import vn.nguongocso.event.service.impl.WarehouseReceiptServiceImpl;
import vn.nguongocso.farm.service.impl.ProductCategoryServiceImpl;
import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;
import vn.nguongocso.recall.service.impl.RecallRequestServiceImpl;
import vn.nguongocso.trace.service.impl.ShipmentServiceImpl;

class ActivityLogEmissionPolicyTest {

    @Test
    void methodsPublishingExplicitActivityEvents_mustNotAlsoBeAuditable() {
        assertNotAudited(ProductionLotServiceImpl.class,
                "createProductionLot",
                "approveProductionLot",
                "submitForApproval",
                "updateProductionLot");

        assertNotAudited(ShipmentServiceImpl.class,
                "createShipment",
                "activateShipmentStamps");

        assertNotAudited(ChainEventServiceImpl.class,
                "recordHarvestEvent",
                "recordPreprocessingEvent",
                "correctPreprocessingEvent",
                "recordPackagingEvent",
                "correctPackagingEvent",
                "recordTransportEvent",
                "recordStorageCondition");

        assertNotAudited(WarehouseReceiptServiceImpl.class, "recordWarehouseReceipt");
        assertNotAudited(ProcurementEventServiceImpl.class, "recordProcurementEvent");
    }

    @Test
    void annotationOnlyActivityLogging_remainsEnabled() {
        assertAudited(ProductCategoryServiceImpl.class, "create", "update");
        assertAudited(RecallRequestServiceImpl.class, "create", "approve", "reject");
    }

    private void assertNotAudited(Class<?> serviceClass, String... methodNames) {
        for (String methodName : methodNames) {
            for (Method method : methodsNamed(serviceClass, methodName)) {
                assertThat(method.isAnnotationPresent(Auditable.class))
                        .as("%s.%s must use only its explicit ActivityLogEvent", serviceClass.getSimpleName(), methodName)
                        .isFalse();
            }
        }
    }

    private void assertAudited(Class<?> serviceClass, String... methodNames) {
        for (String methodName : methodNames) {
            for (Method method : methodsNamed(serviceClass, methodName)) {
                assertThat(method.isAnnotationPresent(Auditable.class))
                        .as("%s.%s must keep annotation-based activity logging", serviceClass.getSimpleName(), methodName)
                        .isTrue();
            }
        }
    }

    private List<Method> methodsNamed(Class<?> serviceClass, String methodName) {
        List<Method> methods = Arrays.stream(serviceClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .toList();

        assertThat(methods)
                .as("Expected method %s.%s to exist", serviceClass.getSimpleName(), methodName)
                .isNotEmpty();
        return methods;
    }
}
