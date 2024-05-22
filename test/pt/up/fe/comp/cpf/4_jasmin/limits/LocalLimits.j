.class public LocalLimits
.super java/lang/Object

.method public func(II)I

	.limit stack 3
	.limit locals 7
	aload_0
	iconst_3
	iconst_4
	invokevirtual LocalLimits/func(II)I
	istore_3
	iconst_3
	iload_3
	iadd
	istore 4
	bipush 10
	iload 4
	imul
	istore 5
	iload_2
	iload 5
	iadd
	istore_1
	aload_0
	iconst_3
	iconst_4
	invokevirtual LocalLimits/func(II)I
	istore 6
	iconst_1
	ireturn
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method