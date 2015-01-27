package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HttpRequest {
  private static final Logger log = Log.getLogger(HttpRequest.class.getName());

  public int http_version_major = 0;
  public int http_version_minor = 0;
  public final Uri uri = new Uri();
  public final Method method = new Method();
  public final Headers headers = new Headers();

  private ByteBuffer bb;

  HttpRequest() {
  }

  static class RequestBuilder {
    static public RequestBuilder newBuilder() {
      return new RequestBuilder();
    }

    private String method;
    private String path;
    private String protocol;
    private List<String> headers = new CopyOnWriteArrayList<String>();

    public RequestBuilder method(String method) {
      this.method = method;
      return this;
    }

    public RequestBuilder path(String path) {
      this.path = path;
      return this;
    }

    public RequestBuilder protocol(String protocol) {
      this.protocol = protocol;
      return this;
    }

    public RequestBuilder addHeader(String name, String value) {
      headers.add(name + ": " + value);
      return this;
    }

    public ByteBuffer[] build() {
      ByteBuffer[] buffers = {
        // Request Line
        ByteBuffer.wrap(method.getBytes()),
        ByteBuffer.wrap(path.getBytes()),
        ByteBuffer.wrap(protocol.getBytes()),
        ByteBuffer.wrap(new String("\r\n").getBytes()),
        // headers
        _headers(),
        ByteBuffer.wrap(new String("\r\n\r\n").getBytes()),
      };

      return buffers;
    }

    private ByteBuffer _headers() {
      int sum = 0;
      for (String header : headers) {
        sum += header.length();
        sum += 2; // \r\n
      }

      ByteBuffer buffer = ByteBuffer.allocateDirect(sum);
      for (String header : headers) {
        buffer.put(ByteBuffer.wrap(header.getBytes()));
        buffer.put(ByteBuffer.wrap(new String("\r\n").getBytes()));
      }

      buffer.flip();
      return buffer;
    }
  }

  static public ByteBuffer[] defaultRequest() {
    return RequestBuilder.newBuilder()
                         .method("GET")
                         .path("/")
                         .protocol("HTTP/1.1")
                         .addHeader("User-Agent", "xio/0.0.0.0.0.0.0.0.0.0.0.0.1")
                         .addHeader("Host", "localhost:8000")
                         .addHeader("Accept", "*/*")
                         .build();
  }

  public void bb(ByteBuffer pbb) {
    bb = pbb.duplicate();
    bb.flip();
  }

  public String method() {
    return method.getMethod();
  }

  public String uri() {
    return uri.getUri();
  }

  public String httpVersion() {
    return new String("HTTP" +
                      "/" +
                      http_version_major +
                      "." +
                      http_version_minor);
  }

  public String getQueryString() {
    String fullString = uri.getUri();
    String qs = fullString.split("?", 1)[1];
    return qs;
  }

  class Picker {
    private int position = -1;
    private int limit = 0;

    public void tick(int currentPos) {
      if (position == -1) {
        position = currentPos;
      }
      limit++;
    }

    public String get() {
      final byte[] value = new byte[limit];
      if (position > 0) {
        bb.position(position);
      } else {
        bb.position(0);
      }
      bb.get(value);
      return new String(value, Charset.forName("UTF-8"));
    }
  }

  class Method extends Picker {
    public String getMethod() {
      return get();
    }
  }

  class Uri extends Picker {
    public String getUri() {
      return get();
    }
  }

  class Header {

    private final Picker name = new Picker();
    private final Picker value = new Picker();
    private Picker currentPicker;

    Header() {
      currentPicker = name;
    }

    public String name() {
      return name.get();
    }

    public String value() {
      return value.get();
    }

    public void tick(int currentPos) {
      currentPicker.tick(currentPos);
    }

    public void startValue() {
      currentPicker = value;
    }
  }

  class Headers {
    private final Deque<Header> header_list = new ArrayDeque<Header>();

    // TODO prefer list of Header objects to minimize String objects at parse
    // time.
    public Map<String, Header> headerMap = new HashMap<String, Header>();
    private Header currentHeader = null;

    Headers() {
    }

    public void newHeader() {
      if (currentHeader != null) {
        done();
      }
      currentHeader = new Header();
    }

    public void done() {
      headerMap.put(currentHeader.name(), currentHeader);
    }

    public void newValue() {
      currentHeader.startValue();
    }

    public void tick(int currentPos) {
      currentHeader.tick(currentPos);
    }

    public boolean empty() {
      return header_list.size() == 0;
    }

    public String get(String name) {
      Header header = headerMap.get(name);
      if (header != null) {
        return header.value();
      } else {
        return "";
      }
    }
  }
}