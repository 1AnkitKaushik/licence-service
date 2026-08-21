package com.licenseservice.dto;

public class K8sServiceActionResponse {
    private String serviceId;
    private String serviceName;
    private String status;
    private String action;

    public K8sServiceActionResponse() {}

    public K8sServiceActionResponse(String serviceId, String serviceName, String status, String action) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.status = status;
        this.action = action;
    }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
