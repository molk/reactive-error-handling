# Error Handling in reactive SpringBoot Services

This project demonstrates how to realise the handling of errors in a [SpringBoot](https://spring.io/projects/spring-boot)
based service pulling data from another service using Spring's [WebClient](https://docs.spring.io/spring/docs/5.0.9.RELEASE/spring-framework-reference/web.html#webmvc-webclient).


## The Architecture

![Service Architecture](/architecture.png)

There are two SpringBoot Maven projects realising two services:

1. `edge-service`
2. `inner-service`

Starting the `edge-service` application fires up a SpringBoot application providing a reactive web service on port `8080`.
The reactive behavior is realised using the APIs of [Project Reactor](https://projectreactor.io/).
So the edge service uses a reactive [WebClient](https://docs.spring.io/spring/docs/5.0.9.RELEASE/spring-framework-reference/web.html#webmvc-webclient)
to call the inner service provided by the Maven project `inner-service`.

The inner service returns a trivial JSON payload in the HTTP response body
and randomly simulates an internal server error by throwing an exception.
This exception is handled in a way to include the message and stacktrace of the causing exception
into a corresponding plain text response along with the HTTP error code `500`.

## The Issue

When using the reactive `WebClient` for accessing a REST API provided by another service, it is important to setup
a decent error handling.

A HTTP GET service call looks like this _one-liner_:

``` java
@GetMapping(path = "/data")
public Mono<Response> data() {

    return webClient.get().uri("/data").retrieve().bodyToMono(Response.class);

}
```

This works fine as long as the called service does not return an HTTP error code.
In case that happens, the default error handling gives a response like the following:

``` bash
$ http GET :8080/data
HTTP/1.1 500 Internal Server Error
Content-Length: 83
Content-Type:  application/json;charset=UTF-8

internal error: ClientResponse has erroneous status code: 500 Internal Server Error
```

So the issue here is the error information returned from the inner service swallowed by the default error handling.
Furthermore, the stacktrace logged doesn't help either, because it provides technical information
logged by the reactor framework, only:


``` java
2018-10-15 20:31:01.977 ERROR 9364 --- [ctor-http-nio-7] demo.service.ExceptionHandlers           : an internal error occurred: ClientResponse has erroneous status code: 500 Internal Server Error

org.springframework.web.reactive.function.client.WebClientResponseException: ClientResponse has erroneous status code: 500 Internal Server Error
    at org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.lambda$createResponseException$7(DefaultWebClient.java:463) ~[spring-webflux-5.0.9.RELEASE.jar:5.0.9.RELEASE]
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:107) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxDefaultIfEmpty$DefaultIfEmptySubscriber.onNext(FluxDefaultIfEmpty.java:92) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxFilterFuseable$FilterFuseableSubscriber.onNext(FluxFilterFuseable.java:104) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1083) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.MonoCollectList$MonoBufferAllSubscriber.onComplete(MonoCollectList.java:117) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onComplete(FluxOnAssembly.java:460) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:130) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onComplete(FluxOnAssembly.java:460) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:130) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.ipc.netty.channel.FluxReceive.terminateReceiver(FluxReceive.java:380) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:204) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.FluxReceive.onInboundComplete(FluxReceive.java:345) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.ChannelOperations.onInboundComplete(ChannelOperations.java:338) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.ChannelOperations.onHandlerTerminate(ChannelOperations.java:404) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:612) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:138) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:102) [netty-codec-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:310) [netty-codec-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:284) [netty-codec-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1434) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:965) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:163) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:628) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:563) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:480) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:442) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884) [netty-common-4.1.29.Final.jar:4.1.29.Final]
    at java.lang.Thread.run(Thread.java:748) [na:1.8.0_181]
    Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.MonoFlatMap] :
    reactor.core.publisher.Mono.flatMap(Mono.java:2081)
    org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.monoThrowableToMono(DefaultWebClient.java:412)
    java.util.Optional.map(Optional.java:215)
    org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.bodyToPublisher(DefaultWebClient.java:440)
    org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.lambda$bodyToMono$0(DefaultWebClient.java:399)
    reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:118)
    reactor.core.publisher.FluxSwitchIfEmpty$SwitchIfEmptySubscriber.onNext(FluxSwitchIfEmpty.java:67)
    reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
    reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:198)
    reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:198)
    reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:198)
    reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:108)
    reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
    reactor.core.publisher.FluxRetryPredicate$RetryPredicateSubscriber.onNext(FluxRetryPredicate.java:81)
    reactor.core.publisher.MonoCreate$DefaultMonoSink.success(MonoCreate.java:146)
    reactor.ipc.netty.channel.PooledClientContextHandler.fireContextActive(PooledClientContextHandler.java:87)
    reactor.ipc.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:584)
    reactor.ipc.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:138)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:102)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438)
    io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:310)
    io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:297)
    io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:413)
    io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:265)
    io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1434)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:965)
    io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:163)
    io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:628)
    io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:563)
    io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:480)
    io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:442)
    io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
Error has been observed by the following operator(s):
    |_	Mono.flatMap(DefaultWebClient.java:412)
    |_	Mono.flatMap(DefaultWebClient.java:398)
    |_	Flux.map(AbstractJackson2Encoder.java:118)
    |_	Mono.from(EncoderHttpMessageWriter.java:110)
    |_	Mono.defaultIfEmpty(EncoderHttpMessageWriter.java:111)
    |_	Mono.flatMap(EncoderHttpMessageWriter.java:112)
```


## The Fix

The origin of this not very helpful standard behaviour is hard to find and can be fixed by adding an error handling
clause to the code describing the reactive inner service call: 

``` java
@GetMapping(path = "/data")
public Mono<Response> data() {

    return webClient.get()
        .uri("/data")
        .retrieve()
        .bodyToMono(Response.class)
        .onErrorMap(
            WebClientResponseException.class,
            e -> new RuntimeException(e.getResponseBodyAsString(), e));

}
```

The error handling lambda will be called in case of `WebClientResponseException` giving us access to the plain text
HTTP body providing the error cause information returned from the inner service, wrapped into an exception
that will be handled instead of the technical reactor exception thrown without this error handling:

``` java
$ http GET :8080/data
HTTP/1.1 500 Internal Server Error
Content-Length: 5284
Content-Type: text/plain

internal error: internal error in inner service: processing error
java.lang.RuntimeException: processing error
        at demo.service.InnerController.someData(InnerController.java:18)
        at demo.service.InnerController.retrieveData(InnerController.java:13)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:209)
        at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:136)
        at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:102)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:891)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:797)
        at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)
        at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:991)
        at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:925)
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:974)
        at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:866)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:635)
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:851)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:742)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:99)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.HttpPutFormContentFilter.doFilterInternal(HttpPutFormContentFilter.java:109)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter.java:93)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:200)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:198)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:493)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:140)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:81)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:87)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:342)
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:800)
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66)
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:806)
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1498)
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.lang.Thread.run(Thread.java:748)
```

Now the edge service logs an even longer stacktrace, but it contains the information that allows us to analyse
what went wrong in the inner service we called:

``` java
java.lang.RuntimeException: internal error in inner service: processing error
java.lang.RuntimeException: processing error
    at demo.service.InnerController.someData(InnerController.java:18)
    at demo.service.InnerController.retrieveData(InnerController.java:13)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke(Method.java:498)
    at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:209)
    at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:136)
    at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:102)
    at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:891)
    at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:797)
    at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)
    at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:991)
    at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:925)
    at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:974)
    at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:866)
    at javax.servlet.http.HttpServlet.service(HttpServlet.java:635)
    at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:851)
    at javax.servlet.http.HttpServlet.service(HttpServlet.java:742)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:99)
    at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.springframework.web.filter.HttpPutFormContentFilter.doFilterInternal(HttpPutFormContentFilter.java:109)
    at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter.java:93)
    at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:200)
    at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:198)
    at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)
    at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:493)
    at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:140)
    at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:81)
    at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:87)
    at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:342)
    at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:800)
    at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66)
    at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:806)
    at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1498)
    at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
    at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
    at java.lang.Thread.run(Thread.java:748)

    at demo.service.EdgeController.lambda$data2$0(EdgeController.java:43) ~[classes/:na]
    at reactor.core.publisher.Mono.lambda$onErrorMap$17(Mono.java:2485) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.Mono.lambda$onErrorResume$19(Mono.java:2577) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:88) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onError(FluxOnAssembly.java:455) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondError(MonoFlatMap.java:185) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.MonoFlatMap$FlatMapInner.onError(MonoFlatMap.java:251) ~[reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onError(FluxOnAssembly.java:455) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:135) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxDefaultIfEmpty$DefaultIfEmptySubscriber.onNext(FluxDefaultIfEmpty.java:92) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxFilterFuseable$FilterFuseableSubscriber.onNext(FluxFilterFuseable.java:104) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:450) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1083) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.MonoCollectList$MonoBufferAllSubscriber.onComplete(MonoCollectList.java:117) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onComplete(FluxOnAssembly.java:460) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:130) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onComplete(FluxOnAssembly.java:460) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:130) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    at reactor.ipc.netty.channel.FluxReceive.terminateReceiver(FluxReceive.java:380) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:204) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.FluxReceive.onInboundComplete(FluxReceive.java:345) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.ChannelOperations.onInboundComplete(ChannelOperations.java:338) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.ChannelOperations.onHandlerTerminate(ChannelOperations.java:404) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:612) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at reactor.ipc.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:138) [reactor-netty-0.7.9.RELEASE.jar:0.7.9.RELEASE]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:102) [netty-codec-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:310) [netty-codec-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:284) [netty-codec-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1434) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:965) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:163) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:628) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:563) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:480) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:442) [netty-transport-4.1.29.Final.jar:4.1.29.Final]
    at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884) [netty-common-4.1.29.Final.jar:4.1.29.Final]
    at java.lang.Thread.run(Thread.java:748) [na:1.8.0_181]
    Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.MonoError] :
    reactor.core.publisher.Mono.error(Mono.java:252)
    reactor.core.publisher.Mono.lambda$onErrorMap$17(Mono.java:2485)
    reactor.core.publisher.Mono.lambda$onErrorResume$19(Mono.java:2577)
    reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:88)
    reactor.core.publisher.MonoFlatMap$FlatMapMain.secondError(MonoFlatMap.java:185)
    reactor.core.publisher.MonoFlatMap$FlatMapInner.onError(MonoFlatMap.java:251)
    reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:135)
    reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
    reactor.core.publisher.FluxDefaultIfEmpty$DefaultIfEmptySubscriber.onNext(FluxDefaultIfEmpty.java:92)
    reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
    reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
    reactor.core.publisher.FluxFilterFuseable$FilterFuseableSubscriber.onNext(FluxFilterFuseable.java:104)
    reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1083)
    reactor.core.publisher.MonoCollectList$MonoBufferAllSubscriber.onComplete(MonoCollectList.java:117)
    reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:130)
    reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:130)
    reactor.ipc.netty.channel.FluxReceive.terminateReceiver(FluxReceive.java:380)
    reactor.ipc.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:204)
    reactor.ipc.netty.channel.FluxReceive.onInboundComplete(FluxReceive.java:345)
    reactor.ipc.netty.channel.ChannelOperations.onInboundComplete(ChannelOperations.java:338)
    reactor.ipc.netty.channel.ChannelOperations.onHandlerTerminate(ChannelOperations.java:404)
    reactor.ipc.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:612)
    reactor.ipc.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:138)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:102)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438)
    io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:310)
    io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:284)
    io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1434)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:965)
    io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:163)
    io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:628)
    io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:563)
    io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:480)
    io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:442)
    io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
Error has been observed by the following operator(s):
    |_	FluxMap$MapSubscriber.onComplete(FluxReceive.java:380)
    |_	Mono.onErrorMap(EdgeController.java:41)
    |_	Flux.map(AbstractJackson2Encoder.java:118)
    |_	Mono.from(EncoderHttpMessageWriter.java:110)
    |_	Mono.defaultIfEmpty(EncoderHttpMessageWriter.java:111)
    |_	Mono.flatMap(EncoderHttpMessageWriter.java:112)

Caused by: org.springframework.web.reactive.function.client.WebClientResponseException: ClientResponse has erroneous status code: 500 Internal Server Error
    at org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.lambda$createResponseException$7(DefaultWebClient.java:463) ~[spring-webflux-5.0.9.RELEASE.jar:5.0.9.RELEASE]
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:107) [reactor-core-3.1.9.RELEASE.jar:3.1.9.RELEASE]
    ... 47 common frames omitted
    Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.MonoFlatMap] :
    reactor.core.publisher.Mono.flatMap(Mono.java:2081)
    org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.monoThrowableToMono(DefaultWebClient.java:412)
    java.util.Optional.map(Optional.java:215)
    org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.bodyToPublisher(DefaultWebClient.java:440)
    org.springframework.web.reactive.function.client.DefaultWebClient$DefaultResponseSpec.lambda$bodyToMono$0(DefaultWebClient.java:399)
    reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:118)
    reactor.core.publisher.FluxSwitchIfEmpty$SwitchIfEmptySubscriber.onNext(FluxSwitchIfEmpty.java:67)
    reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
    reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:198)
    reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:198)
    reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:198)
    reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:108)
    reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
    reactor.core.publisher.FluxRetryPredicate$RetryPredicateSubscriber.onNext(FluxRetryPredicate.java:81)
    reactor.core.publisher.MonoCreate$DefaultMonoSink.success(MonoCreate.java:146)
    reactor.ipc.netty.channel.PooledClientContextHandler.fireContextActive(PooledClientContextHandler.java:87)
    reactor.ipc.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:584)
    reactor.ipc.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:138)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:102)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438)
    io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:310)
    io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:297)
    io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:413)
    io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:265)
    io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
    io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1434)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
    io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
    io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:965)
    io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:163)
    io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:628)
    io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:563)
    io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:480)
    io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:442)
    io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
Error has been observed by the following operator(s):
    |_	Mono.flatMap(DefaultWebClient.java:412)
    |_	Mono.flatMap(DefaultWebClient.java:398)
```

### Media Type fix

If not set explicitly the framework will use `application/json;charset=UTF-8` even for plain text content:

``` bash
$ http GET :8080/data
HTTP/1.1 500 Internal Server Error
Content-Length: 83
Content-Type:  application/json;charset=UTF-8

internal error: ClientResponse has erroneous status code: 500 Internal Server Error
```

Providing an exception handler allows us to get a foot into the door to set the media type for plain text
error detail payloads:

``` java
@ControllerAdvice
class ExceptionHandlers {

    @ExceptionHandler(Exception.class)
    ResponseEntity<String> handleInternalServerError(Exception cause) {

        return status(INTERNAL_SERVER_ERROR)
            .contentType(MediaType.TEXT_PLAIN)
            .body(format("internal error: %s", cause.getMessage()));

    }

}
```

