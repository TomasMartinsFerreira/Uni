package pt.tecnico.blockchainist.node.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * HeaderNodeInterceptor is a gRPC server interceptor that intercepts incoming gRPC calls to the NodeService and checks for a custom header "node-delay".
 * If the header is present, it extracts the value, which represents a delay in seconds,
 * and makes the node thread sleep for that duration before proceeding with the call.
 * This allows us to simulate network latency or processing delays in the NodeService for testing purposes.
 */
public class HeaderNodeInterceptor implements ServerInterceptor {

  //Ensure that the name of the header key to match the header key used by the sender.
  static final Metadata.Key<String> CUSTOM_HEADER_KEY =
      Metadata.Key.of("node-delay", Metadata.ASCII_STRING_MARSHALLER);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call,
      final Metadata requestHeaders,
      ServerCallHandler<ReqT, RespT> next) {
        
        String nodeDelay = requestHeaders.get(CUSTOM_HEADER_KEY);

        if (nodeDelay != null) {
            try {
                int delay = Integer.parseInt(nodeDelay);
                Thread.sleep(delay*1000); // Convert seconds to milliseconds
            } catch (NumberFormatException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        return next.startCall(call, requestHeaders);
  }
}
