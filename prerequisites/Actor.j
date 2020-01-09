.class public Actor
.super DefaultActor

.field private queue Ljava/util/ArrayList;
.signature "Ljava/util/ArrayList<LMessage;>;"
.end field
.field private lock Ljava/util/concurrent/locks/ReentrantLock;
.end field
.field queueSize I
.end field

.method public <init>(I)V
.limit stack 3
.limit locals 2
aload_0
invokespecial DefaultActor/<init>()V
aload_0
new java/util/ArrayList
dup
invokespecial java/util/ArrayList/<init>()V
putfield Actor/queue Ljava/util/ArrayList;
aload_0
new java/util/concurrent/locks/ReentrantLock
dup
invokespecial java/util/concurrent/locks/ReentrantLock/<init>()V
putfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;
aload_0
iload_1
putfield Actor/queueSize I
return
.end method

.method public run()V
.limit stack 2
.limit locals 2
Label0:
aconst_null
astore_1
aload_0
getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;
invokevirtual java/util/concurrent/locks/ReentrantLock/lock()V
aload_0
getfield Actor/queue Ljava/util/ArrayList;
invokevirtual java/util/ArrayList/isEmpty()Z
ifne Label31
aload_0
getfield Actor/queue Ljava/util/ArrayList;
iconst_0
invokevirtual java/util/ArrayList/remove(I)Ljava/lang/Object;
checkcast Message
astore_1
Label31:
aload_0
getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;
invokevirtual java/util/concurrent/locks/ReentrantLock/unlock()V
aload_1
ifnull Label46
aload_1
invokevirtual Message/execute()V
Label46:
goto Label0
.end method

.method public send(LMessage;)V
.limit stack 2
.limit locals 2
aload_0
getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;
invokevirtual java/util/concurrent/locks/ReentrantLock/lock()V
aload_0
getfield Actor/queue Ljava/util/ArrayList;
invokevirtual java/util/ArrayList/size()I
aload_0
getfield Actor/queueSize I
if_icmpge Label30
aload_0
getfield Actor/queue Ljava/util/ArrayList;
aload_1
invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z
pop
Label30:
aload_0
getfield Actor/lock Ljava/util/concurrent/locks/ReentrantLock;
invokevirtual java/util/concurrent/locks/ReentrantLock/unlock()V
return
.end method


