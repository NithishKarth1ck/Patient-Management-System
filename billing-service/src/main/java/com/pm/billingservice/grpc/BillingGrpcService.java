package com.pm.billingservice.grpc;

import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.pm.billingservice.service.BillingService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);
    private final BillingService billingService;

    public BillingGrpcService(BillingService billingService){
        this.billingService = billingService;
    }
    @Override
    public void createBillingAccount(billing.BillingRequest request,
    StreamObserver<billing.BillingResponse> responseObserver){

        log.info("createBillingAccount request received for patientId: {}", request.getPatientId());

        BillingResponse response = billingService.createBillingAccount(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
