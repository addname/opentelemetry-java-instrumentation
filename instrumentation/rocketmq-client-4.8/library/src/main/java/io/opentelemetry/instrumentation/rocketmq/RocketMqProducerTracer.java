/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rocketmq;

import static io.opentelemetry.api.trace.SpanKind.PRODUCER;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

public class RocketMqProducerTracer extends BaseTracer {

  private static final RocketMqProducerTracer TRACER = new RocketMqProducerTracer();

  public static RocketMqProducerTracer tracer() {
    return TRACER;
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.javaagent.rocketmq-client";
  }

  public Context startProducerSpan(Context parentContext,String addr, Message msg) {
    SpanBuilder spanBuilder =
        spanBuilder(spanNameOnProduce(msg), PRODUCER).setParent(parentContext);
    onProduce(spanBuilder, msg, addr);
    return parentContext.with(spanBuilder.startSpan());
  }

  private void onProduce(SpanBuilder spanBuilder, Message msg, String addr) {
    spanBuilder.setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketmq");
    spanBuilder.setAttribute(SemanticAttributes.MESSAGING_DESTINATION_KIND, "topic");
    spanBuilder.setAttribute(SemanticAttributes.MESSAGING_DESTINATION, msg.getTopic());
    if (RocketMqClientConfig.CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES) {
      spanBuilder.setAttribute("messaging.rocketmq.tags", msg.getTags());
      spanBuilder.setAttribute("messaging.rocketmq.broker_address", addr);
    }
  }

  public void afterProduce(Context context, SendResult sendResult) {
    Span span = Span.fromContext(context);
    span.setAttribute(SemanticAttributes.MESSAGING_MESSAGE_ID, sendResult.getMsgId());
    if (RocketMqClientConfig.CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES) {
      span.setAttribute("messaging.rocketmq.send_result", sendResult.getSendStatus().name());
    }
  }

  private String spanNameOnProduce(Message msg) {
    return msg.getTopic() + " send";
  }
}
