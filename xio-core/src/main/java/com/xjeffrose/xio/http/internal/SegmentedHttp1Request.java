package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.SegmentedRequest;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import lombok.ToString;

/** Wrap an incoming HttpResponse, for use in a server. */
@ToString
public class SegmentedHttp1Request implements SegmentedRequest {

  protected final HttpRequest delegate;
  private final Http1Headers headers;
  private final TraceInfo traceInfo;

  public SegmentedHttp1Request(HttpRequest delegate, TraceInfo traceInfo) {
    this.delegate = delegate;
    this.headers = new Http1Headers(delegate.headers());
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
  }

  public SegmentedHttp1Request(HttpRequest delegate) {
    this(delegate, null);
  }

  // region Request

  @Override
  public boolean startOfMessage() {
    return true;
  }

  @Override
  public boolean endOfMessage() {
    return false;
  }

  @Override
  public HttpMethod method() {
    return delegate.method();
  }

  @Override
  public String path() {
    return delegate.uri();
  }

  @Override
  public String version() {
    return delegate.protocolVersion().text();
  }

  @Override
  public Headers headers() {
    return headers;
  }

  @Override
  public int streamId() {
    return H1_STREAM_ID_NONE;
  }

  @Override
  public boolean keepAlive() {
    return HttpUtil.isKeepAlive(delegate);
  }

  @Override
  public ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }

  // endregion

  // region Traceable

  @Override
  public TraceInfo httpTraceInfo() {
    return traceInfo;
  }

  // endregion

}
