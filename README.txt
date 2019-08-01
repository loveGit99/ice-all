
1. 功能说明：
		1> ice-all 提供了一套ICE服务端、客户端的注解式通用组件。
		2> @ICEServer    发布ICE服务
		3> @ICEProvider  调用ICE服务
		

2. 使用约束：
		1> 该组件是基于spring4.x的框架开发的，并且是通过spring bean的注解方式实现的，所以只能适用于spring 注解方式构建的工程。
		2> 第一版暂时不支持SSL功能。
	

3. 模块说明:
		1> ice-commons:	   ICE 组件依赖基础
		1> ice-customer:   ICE 客户端组件， 使用 @ICEProvider 需要依赖该jar包
		2> ice-server ：   ICE 服务发布组件， 使用 @ICEServer 需要依赖该jar包

		
		
4. ICE 客户端组件使用说明：
		1> 注解：  
			   定义：     @ICEProvider([name=""], system="")
			   使用对象： spring bean 的成员变量; ICE 发布服务的接口类(注意:不是自己编写的实现类)。
		             参数说明：
		                >> name：ICE服务发布的服务名称， 如果不设置，则服务名默认和被注释的变量类名一致，但首字母小写。
		                >> system : ICE 服务的endpoints，在common包中有一个常量类(com.hhly.sns.biz.utils.constants.EndpointConstant)，直接引用即可
		             
		2> XML 配置
               项目中需要增加对ice-customer组件的扫描，包括基础IOC容器和web IOC容器。
               特别对于spring-mvc 中web模块中使用该注解，需要增加对组件的扫描，例如(下例中的com.hhly.common.components.ice.customer）：
                <context:component-scan base-package="XXXXXXXX.controller,com.hhly.common.components.ice.customer" />
			   	
        3> properties 配置：
			  3.1  公用配置文件：  ice-registry.properties
			        a. 服务端和客户端都需要配置的
			        b. 配置内容如下：
			                 # ice registry center node path setting.
			                 # Notes: Don't modify this configuration without specific reason.
                             ice.endpoints.node=/ice-services-endpoints

                             # release version setting.
                             service.release.version=dev

             3.2 ICE 服务端配置：  ice-server.properties
                    a. 配置内容如下：
                            #[required] server name, for example : user, city,video, info.
                            ice.system.name=user

                            # [optional]server endpoint setting
                            # If you want to set the ice endpoint instead of system automatic allocation,
                            # you can open the following setting.
                            ice.servers.endpoints=tcp -h 192.168.31.100 -p 5050


                            #[optional]  0: not registry ( local debug model),  1: registry into zk
                            # Notes:  You must set it in your IDE ENV ( eclipse or IDEA and so on)
                            ice.servers.exposed=0

             3.3 ICE 客户端配置： ice-client-example.properties
                   a. 配置内容如下：
                            #  request timeout setting. the unit is millisecnd
                            ice.provider.timeout.ms=5000

                            # special the ice server endpoint for local debug usually.
                            ice.provider.endpoints.sns.[服务名称].default=tcp -h 192.168.31.103 -p 5050

                            #例如   ice.provider.endpoints.sns.user.default=tcp -h 192.168.31.103 -p 5050

            3.4  配置中心设置：  zk.properties
                    a. 配置内容如下：
                            #[required]  zookeeper cluster setting
                            zookeeper.address=192.168.10.44:2182,192.168.10.45:2182,192.168.10.46:2182

			  
       4> 示例：
               @Component
			   public class ClientExample {
	
					@ICEProvider(name="member", system=EndpointConstant.SNS_USER)
					private Member  member;	 


                   public String getMemberInfo(){
						String params = "xxxxxxx";
						return member.memberOperation(pams);						
				   }					
		       }
			 

			 
			 
5. ICE 服务端组件使用说明：
		1>  注解：  
				定义：		@ICEServer([name=""]) 
	
				使用对象：   类 
			   
				参数说明： 	name :  发布的ICE 服务名称，如果不设置，则服务名默认和被注释的变量名称一致，但首字母小写。 

		2>  properties 配置:
				# 发布的服务的 endpoints
                ice.servers.endpoints=..............	
				# 例子  tcp -h 192.168.32.86 -p 50000
				
		3>  示例：
				@Component
				@ICEServer(name = "printer")
				public class PrinterImpl extends _PrinterDisp {
				
				 //实现接口的业务代码
				 ...................
				 ..................
				
				}
				


6. Maven 依赖配置

		  <dependency>
			<groupId>com.hhly.common.components.ice</groupId>
			<artifactId>ice-customer</artifactId>
			<version>xxxx-SNAPSHOT</version>
		  </dependency>
		  <dependency>
			<groupId>com.hhly.common.components.ice</groupId>
			<artifactId>ice-server</artifactId>
			<version>xxx-SNAPSHOT</version>
		  </dependency>				
      				
				
			 
