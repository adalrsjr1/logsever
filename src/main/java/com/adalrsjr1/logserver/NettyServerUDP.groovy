package com.adalrsjr1.logserver


import groovy.util.logging.Slf4j
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.util.ReferenceCountUtil

@Slf4j
class MessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		
		ByteBuf input = (ByteBuf) msg.content()
		msg.retain()
		
		StringBuffer sb = new StringBuffer()
		while(input.isReadable()) {
			char c = (char) input.readByte()
			sb.append(c != '\n' ? c : '')
		}
		
		out.add(sb.toString())
	}
}

@Slf4j
class NettyServerUDPHandler extends ChannelInboundHandlerAdapter {
	File trace
	boolean logConsole
	
	NettyServerUDPHandler(File traceFile, boolean logConsole) {
		trace = traceFile
		this.logConsole = logConsole
	}
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		try {
			trace << "${msg}\n"
			if(logConsole)
				log.info "${(String) msg}"
		}
		finally {
			ReferenceCountUtil.release(msg)
		}
	}
}

class NettyServerUDP {
	private static final OS = System.getProperty("os.name").toLowerCase()
	private int port
	private File traceFile
	private boolean logConsole

	NettyServerUDP(String traceFile, boolean logConsole) {
		this(9999, traceFile, logConsole)
	}
	
	NettyServerUDP(int port, String traceFile, boolean logConsole) {
		this.port = port
		this.traceFile = new File(traceFile)
		this.logConsole = logConsole
		trace.createNewFile()
	}

	File getTrace() {
		traceFile
	}
	
	void run() throws Exception {
		EventLoopGroup group = OS.indexOf("nix") >= 0 ? new EpollEventLoopGroup() : new NioEventLoopGroup()
		try {
			Bootstrap bootstrap = new Bootstrap()
			Class clazz = OS.indexOf("nix") >= 0 ? EpollDatagramChannel.class : NioDatagramChannel.class
			bootstrap.group(group)
					.channel(clazz)
					.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.handler(new ChannelInitializer() {
						void initChannel(Channel ch) {
							ch.pipeline().addLast(new MessageDecoder(), new NettyServerUDPHandler(traceFile, logConsole))
						}
					})

			ChannelFuture f = bootstrap.bind(port).sync()
			f.channel().closeFuture().sync()
		}
		finally {
			group.shutdownGracefully()
		}

	}

	public static void main(String[] args) throws Exception {
		new NettyServerUDP("app.trace", false).run()
	}

}
