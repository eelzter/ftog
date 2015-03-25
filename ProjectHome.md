Flex Data Transfer Object Generator generates DTO actionscript source from java source.

In a Flex DS/LCDS/BlazeDS-project a good pattern is to have data only transfer objects sent from the server over RPC or in RTMP-messages to the client. This tool lets you generate corresponding actionscript transfer classes for the client side from java source so that your server side objects keep being "hard typed" on the client side as well.

The tool is an ant-task.