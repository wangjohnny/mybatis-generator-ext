#项目介绍
这是一个针对 [mybatis generator](https://github.com/mybatis/generator) 的扩展项目, 后续会用 MBG 来指代原始的 mybatis generator项目

#问题
描述：使用原始的 MBG 在开发过程中，一旦修改数据库，就需要重新生成 Mapper 文件与 Model 类。
但是我们在开发过程中，可能已经修改了以前自动生成的代码与xml文件，我经过 N 多项目的实践，总结出一套理论思路：生成的不修改，修改的不生成。随着需求的变化，可以不断使用自动生成，并且程序员在需要扩展的地方，有可以编写自己的扩展。这个方法，在互联网领域很有价值，因为大部分的项目，需求都是不断变化的。

#思路与解决方法
简单来说，需要利用 mybatis 的一些特点来解决问题，比如：可以有多个 xml 文件，model 可以继承。利用这个特点，我们通过 MBG 的 plugin 来改造 MGB 生成的文件。最终的产物是三个 java 类（两个 Model 类，1个 Mapper 类），两个 xml 文件

#生成的xml 文件与 java 代码结构
##用 user 表来做示例

1. 生成两个 java 类：User 与 BaseUser，User 继承 BaseUser，BaseUser 继承 BaseModel。BaseModel由本工具提供一个基础类，包含几个基础字段。BaseUser 不可以修改，BaseUser 类是原始的 MBG 工具生成的代码，这是通过 plugin 的机制修改的类名，后续本工具再生成代码的时候，会自动覆盖这个文件。User 可以修改，可以添加自己的属性与方法，后续工具再生成不会重新生成这个文件

2. xml 文件：两个 xml 文件名相同，只是会放在不同的 package 下，一个是 MBG 自动生成的文件，每次覆盖。一个是留给程序员修改的文件，不再覆盖。

#示例代码
[mybatis generator demo](https://github.com/wangjohnny/mbg-demo-parent)
