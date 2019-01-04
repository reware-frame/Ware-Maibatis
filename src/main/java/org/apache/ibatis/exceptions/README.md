## exceptions 笔记

1. 使用异常工厂 ExceptionFactory 将通用异常包装成自己的项目异常

2. 异常往往采用继承结构，顶级父类往往是RuntimeException

3. exceptions包定义基类异常，每个包都继承基类异常来定义自己的模块异常