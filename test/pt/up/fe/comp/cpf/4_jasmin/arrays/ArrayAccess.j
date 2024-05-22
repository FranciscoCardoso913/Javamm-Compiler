.class public ArrayAccess
.super java/lang/Object

.method public static main([Ljava/lang/String;)V

	.limit stack 3
	.limit locals 7
	iconst_5
	newarray int
	astore_1
	aload_1
	iconst_0
	iconst_1
	iastore
	aload_1
	iconst_1
	iconst_2
	iastore
	aload_1
	iconst_2
	iconst_3
	iastore
	aload_1
	iconst_3
	iconst_4
	iastore
	aload_1
	iconst_4
	iconst_5
	iastore
	aload_1
	iconst_0
	iaload
	istore_2
	iload_2
	invokestatic ioPlus/printResult(I)V
	aload_1
	iconst_1
	iaload
	istore_3
	iload_3
	invokestatic ioPlus/printResult(I)V
	aload_1
	iconst_2
	iaload
	istore 4
	iload 4
	invokestatic ioPlus/printResult(I)V
	aload_1
	iconst_3
	iaload
	istore 5
	iload 5
	invokestatic ioPlus/printResult(I)V
	aload_1
	iconst_4
	iaload
	istore 6
	iload 6
	invokestatic ioPlus/printResult(I)V
	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method