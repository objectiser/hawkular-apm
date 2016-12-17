TODO:

Currently service name is supplied based on the deployment metadata. But if the app specifies as tag on span, then
it won't be used. Need SpanBuilder to make the tags (init before start) made available when doing createSpan, as with
the references.

Currently TraceListener references APMSpan, which is in io.opentracing package - but ideally needs to be in
client.opentracing package - so may need some abstract form in TraceListener.

Need clean up thread to work through sessions seeing why they have completed
and if in terminating state - and if not reporting an error - but where?
