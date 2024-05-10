.class public ArrayAsArg
.super java/lang/Object

.method public func([I)I

	.limit stack 2
	.limit locals 5
	new ArrayAsArg
	astore_2
	aload_2
	invokespecial ArrayAsArg/<init>()V
	iconst_2
	newarray int
	astore_3
	aload_2
	aload_3
	invokevirtual ArrayAsArg/func([I)I
	istore 4
	iload 4
	ireturn
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method