/*-
 * -\-\-
 * protoman-registry
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

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
