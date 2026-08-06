package com.licenseservice.model;

public class K8sServiceActionResponse {

    private String serviceId;
    private String serviceName;
    private LicenseState status;
    private ActionRequired action;

    public K8sServiceActionResponse(String serviceId, String serviceName, LicenseState status, ActionRequired action) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.status = status;
        this.action = action;
    }

    public String getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public LicenseState getStatus() { return status; }
    public ActionRequired getAction() { return action; }
}