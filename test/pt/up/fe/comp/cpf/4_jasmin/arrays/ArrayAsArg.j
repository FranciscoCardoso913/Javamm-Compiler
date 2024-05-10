.class public ArrayAsArg
.super java/lang/Object

.method public func([I)I

	.limit stack 1
	.limit locals 3
	aload_1
	arraylength
	istore_2
	iload_2
	ireturn
.end method

.method public func2()I

	.limit stack 2
	.limit locals 4
	new ArrayAsArg
	astore_1
	aload_1
	invokespecial ArrayAsArg/<init>()V
	iconst_2
	newarray int
	astore_2
	aload_1
	aload_2
	invokevirtual ArrayAsArg/func([I)I
	istore_3
	iload_3
	ireturn
.end method

.method public static main([Ljava/lang/String;)V

	.limit stack 1
	.limit locals 3
	new ArrayAsArg
	astore_1
	aload_1
	invokespecial ArrayAsArg/<init>()V
	aload_1
	invokevirtual ArrayAsArg/func2()I
	istore_2
	iload_2
	invokestatic ioPlus/printResult(I)V
	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method