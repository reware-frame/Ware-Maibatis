## parsing

将#{}，${}中的内容，根据properties文件的<key,value>，替换为相应的value值。

对JDK或者其他组件封装一下，然后提供自己的实现方法。

### TokenHandler 记号处理器 接口

这个接口中只有一个函数，就是对字符串进行处理。

### GenericTokenParser 普通记号解析器，处理#{}和${}参数

简单的说，这个函数的作用就是将openToken(开始记号)和endToken(结束记号)间的字符串取出来用handler处理下，然后再拼接到一块。

（占位符：设置openToken为'${',endToken为'}'）

内置了一个抽象TokenHandler处理器，通过外部实现（具体解析文件的实现）进行注入。

底层逻辑为将string转化为 **char[]**，然后进行字符循环解析，通过StringBuilder进行字符拼接。

### PropertyParser 属性解析器

### XPathParser

> 设计思路：当构造函数数量多时，将实例变量的设置抽象到外部方法，同时进行init方法，这样大量的构造函数调用此共同构造方法即可。

> XPath解析器，用的都是JDK的类包,封装了一下，使得使用起来更方便

XPathParser类parsing包中的核心类之一，既然这个类是XPath的Parser，就需要对xpath的语法有所了解，如果对此不熟悉的读者最好能先了解xpath的语法（http://www.w3school.com.cn/xpath/index.asp）。

打开这个类的outline会发现这个类包含的函数真的是“蔚为壮观”，虽然数量众多，基本上可以分为两类：初始化（构造函数）、evalXXX。

运行流程：

1. 需要两个文件来进行替换解析：（properties资源变量对象）和（document文档对象）

2. properties通过外部解析对象进行传入，document文档对象通过构造函数注入。

在构造过程中，同时要设置其他对象，构建 **DocumentBuilderFactory** 文档解析工厂，需要设置 **EntityResolver** 实体解析器。

需要注意的就是定义了EntityResolver(XMLMapperEntityResolver)，这样不用联网去获取DTD，
将DTD放在org\apache\ibatis\builder\xml\mybatis-3-config.dtd,来达到验证xml合法性的目的

3. 将document通过xpath进行解析，然后转为string字符串 

4. 将properties的资源变量和string资源字符串传入PropertyParser解析器进行解析。

5. 返回解析后的字符串

### XNode 节点

该类是对org.w3c.dom.Node类的一个封装，在Node类的基础上添加了一些新功能。

解析XML的节点数据。
