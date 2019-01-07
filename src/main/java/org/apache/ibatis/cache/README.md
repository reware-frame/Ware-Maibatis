## cache 缓存

### CacheKey 计算缓存对象的 key

MyBatis 对于其 Key 的生成采取规则为：[mappedStementId + offset + limit + SQL + queryParams + environment]生成一个哈希码

------------------------------------------

使用委托模式，只维护一个集合，其他的委托给Cache执行

### 1. FIFO 先进先出缓存

使用 **linkedlist** 链表维护，若长度超出，则删除首部元素

### 2. LRU 最近最少使用缓存，移除最长时间不被使用的对象

使用 **LinkedHashMap** 维护，LinkedHashMap内部其实就是每次访问或者插入一个元素都会把元素放到链表末尾。那么最长时间不被使用的对象就是表头。

实现 LinkedHashMap 的 removeEldestEntry 方法，返回最老元素的 key 值。

若长度超出，返回最老元素的 key 值，检查若 key 不为空，则删除此 key 对象

### 3. Soft 软引用缓存，核心是SoftReference，移除基于垃圾回收器状态和软引用规则的对象

如果一个对象只具有软引用，则内存空间足够，垃圾回收器就不会回收它；如果内存空间不足了，就会回收这些对象的内存。
只要垃圾回收器没有回收它，该对象就可以被程序使用。软引用可用来实现内存敏感的高速缓存。   

> SoftReference的特点是它的一个实例保存对一个Java对象的软引用，该软引用的存在不妨碍垃圾收集线程对该Java对象的回收。
也就是说，一旦SoftReference保存了对一个Java对象的软引用后，在垃圾线程对这个Java对象回收前，
SoftReference类所提供的get()方法返回Java对象的强引用。另外，一旦垃圾线程回收该Java对象之后，get()方法将返回null。 

维护两个队列，一个是软引用的队列 **ReferenceQueue** ，用来保存软引用对象，他们的值可能存在也可能失效。

第二个是强引用的队列 **Deque** ， 用来保存访问过的元素（视为经常访问的元素），当容量满时删除最早入队的元素。

1. 当插入软引用到软引用缓存队列时，首先遍历删除值失效的软引用对象，然后将软引用对象插入队列。

2. 当访问软引用对象时，若已经失效，则删除元素返回null，若值未被垃圾回收，那么将软引用对象升级成为强引用对象（加入到强引用队列），同时返回其值

若强引用队列满时，删除最早插入队列的值。

### 4. Weak 弱引用缓存，可以看到代码和SoftCache如出一辙，就是SoftReference变成了WeakReference
         
弱引用与软引用的区别在于：只具有弱引用的对象拥有更短暂的生命周期。在垃圾回收器线程扫描它所管辖的内存区域的过程中，
一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存。
不过，由于垃圾回收器是一个优先级很低的线程，因此不一定会很快发现那些只具有弱引用的对象。         
         
> 更积极移除基于垃圾收集器状态和弱引用规则的对象

----------------------------------------

### BlockingCache 阻塞缓存模型

当元素在缓存中找不到时，它会在缓存键上设置一个锁。这样，其他线程将等待这个元素被填充，而不是命中数据库。

使用 **ConcurrentHashMap** 对缓存对象进行加锁保护，锁机制采用 **ReentrantLock**。

### LoggingCache 日志缓存，添加功能：取缓存时打印命中率

使用mybatis自己的抽象Log

维护两个值，访问次数和命中次数，打印命中率。

### ScheduledCache 定时调度缓存，目的是每一小时（默认）清空一下缓存

使用 **System.currentTimeMillis()** 进行时间计算

### SerializedCache 序列化缓存，用途是先将对象序列化成2进制，再缓存,好处是将对象压缩了，省内存。坏处是速度慢了

序列化： Object -> byte[]
    
    //序列化核心就是ByteArrayOutputStream
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(value);
    oos.flush();
    oos.close();
    return bos.toByteArray();

反序列化： byte[] -> Object

    //反序列化核心就是ByteArrayInputStream
    ByteArrayInputStream bis = new ByteArrayInputStream(value);
    ObjectInputStream ois = new CustomObjectInputStream(bis);
    result = (Serializable) ois.readObject();
    
### SynchronizedCache 同步缓存，

    防止多线程问题
    核心: 加锁
    ReadWriteLock.readLock().lock()/unlock()
    ReadWriteLock.writeLock().lock()/unlock()
    3.2.6以后这个类已经没用了，考虑到Hazelcast, EhCache已经有锁机制了，所以这个锁就画蛇添足了。
    bug见https://github.com/mybatis/mybatis-3/issues/159
    
### （仍有疑问）TransactionalCache 事务缓存，一次性存入多个缓存，移除多个缓存

底层使用 Map 管理 entriesToAddOnCommit（要添加到Commit上的条目），每次put时都提交到此处。

使用 Set 管理 entriesMissedInCache（缓存中遗漏的条目），每次get时，若未命中，则添加到此处。

设置标志位: commit时要不要清缓存，默认commit时不清缓存

commit方法，提供事务功能，将Map中的条目全部提交给委托的Cache类，然后检查MISS中遗漏的项，将其全部添加给Cache类。

rollback方法，提供事务回滚功能，将未命中条目全部提交。

### TransactionalCacheManager 事务缓存管理器，被CachingExecutor使用

通过Map管理了许多TransactionalCache

提交时全部提交

回滚时全部回滚

### PerpetualCache 永久缓存，一旦存入就一直保持

每个永久缓存有一个ID来识别，需要实现hashcode()和equals()

内部就是一个HashMap,所有方法基本就是直接调用HashMap的方法,不支持多线程