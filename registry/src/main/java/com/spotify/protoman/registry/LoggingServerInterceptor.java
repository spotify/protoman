package com.spotify.protoman.registry;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggingServerInterceptor implements ServerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(LoggingServerInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      final ServerCall<ReqT, RespT> call,
      final Metadata metadata,
      final ServerCallHandler<ReqT, RespT> next) {

    logger.debug("Inbound: [{}]", call.getMethodDescriptor().getFullMethodName());

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
        next.startCall(
            new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
              @Override
              protected ServerCall<ReqT, RespT> delegate() {
                return super.delegate();
              }

              @Override
              public void sendMessage(final RespT message) {
                logger.debug("sendMessage: {}", format(message));
                super.sendMessage(message);
              }

              @Override
              public void close(final Status status, final Metadata trailers) {
                logger.debug("Closed. status={} metadata={}", status, trailers);
                super.close(status, trailers);
              }
            },
            metadata)) {

      @Override
      public void onHalfClose() {
        logger.debug("Client finished sending messages");
        super.onHalfClose();
      }

      @Override
      public void onMessage(final ReqT message) {
        logger.debug("onMessage: {}", format(message));
        super.onMessage(message);
      }

      @Override
      public void onCancel() {
        logger.warn("Call canceled");
        super.onCancel();
      }

      @Override
      public void onComplete() {
        logger.debug("Call complete");
        super.onComplete();
      }

    };

  }

  public static String format(Object obj) {
    return obj instanceof Message ? TextFormat.printToString((Message) obj) : "n/a";
  }
}
