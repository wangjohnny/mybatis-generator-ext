# 项目介绍
本项目是[mybatis-generator-ext](https://github.com/wangjohnny/mybatis-generator-ext)的示例，为了简单，数据库里只有一个表，只用了一个 user 表。项目使用了 maven 常见的目录分类，parent 目录下除了常见的 model、mapper、business(有人用 service)、webapp，我添加了一个代码生成项目mbg-demo-generator，一般命名模式是“项目名-generator”。有的项目小，不区分那么多项目，直接放在一个项目里，工具也是也支持配置的。

# 注意事项
为了演示方便，demo 里使用了hsql，可以修改为你想要的任何数据库（涉及到数据库的分页，由于每种数据库的不同，需要修改分页插件，目前的分页插件只支持 mysql 与 hsql，别的没测试过，后续会添加 sql server 与 oracle）

# 关键配置文件
generatorConfig.properties：数据库连接信息与代码生成的路径

generatorConfig.xml：MBG 的标准配置文件，具体请 mybatis generator 的官方文档

# 演示代码生成
执行 mbg-demo-generator 项目下 mybatis-generator.sh。

* 尝试修改子类(Model 与 Mappper)，重新自动生成;
* 尝试删除生成代码(Model 与 Mapper)，重新生成代码;

通过以上方式，来体验代码生成的方式。

# web方式演示
在命令行下，执行 mbg-demo-webapp目录下的 run.sh，通过 maven 来执行 tomcat 插件，然后通过浏览器访问http://localhost:8080/demo/user/list，就可以看到演示数据了

# 注意事项
> 命令脚本都是 mac 下写的，没在其它平台测试过。linux 估计不需要修改，windows可能需要。
