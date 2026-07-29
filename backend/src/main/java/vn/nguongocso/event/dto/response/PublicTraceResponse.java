package vn.nguongocso.trace.dto.response;
import java.util.List;

public class PublicTraceResponse{
    private String codeValue;
    private String productName;
    private String shipmentCode;
    private String shipmentStatus;
    private Boolean recalled;
    private String recallMessage;
    private List<PublicChainEventItem> events;
}