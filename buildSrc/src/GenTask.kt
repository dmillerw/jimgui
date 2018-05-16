package org.ice1000.gradle

import org.gradle.api.tasks.TaskAction
import org.intellij.lang.annotations.Language

@Suppress("PrivatePropertyName", "LocalVariableName", "FunctionName")
abstract class GenTask(
		className: String,
		private val `c++FileSuffix`: String
) : GenJavaTask(className), Runnable {
	init {
		group = "code generation"
	}

	private val `prefixC++`
		@Language("C++")
		get() = """$CXX_PREFIX
#include <org_ice1000_jimgui_$className.h>
"""

	@TaskAction
	override fun run() {
		val `targetC++File` = project
				.projectDir
				.resolve("jni")
				.resolve("generated_$`c++FileSuffix`.cpp")
				.absoluteFile
		targetJavaFile.parentFile.mkdirs()
		`targetC++File`.parentFile.mkdirs()
		val javaCode = StringBuilder(prefixJava)
		java(javaCode)
		javaCode.append(eol).append('}')
		targetJavaFile.writeText("$javaCode")
		val cppCode = StringBuilder(`prefixC++`)
		`c++`(cppCode)
		cppCode.append(CXX_SUFFIX)
		`targetC++File`.writeText("$cppCode")
	}

	abstract fun `c++`(cppCode: StringBuilder)
	abstract fun java(javaCode: StringBuilder)

	fun <T> List<T>.joinLinesTo(builder: StringBuilder, transform: (T) -> CharSequence) = joinTo(builder, eol, postfix = eol, transform = transform)

	fun `c++BooleanAccessor`(name: String, additionalParamText: String = "") =
			"""JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_is$name(JNIEnv *, jobject $additionalParamText) -> jboolean { return static_cast<jboolean> ($`c++Expr`$name ? JNI_TRUE : JNI_FALSE); }
JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_set$name(JNIEnv *, jobject $additionalParamText, jboolean newValue) -> void { $`c++Expr`$name = newValue; }"""

	fun `c++PrimitiveAccessor`(type: String, name: String, additionalParamText: String = "") =
			"""JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_set$name(JNIEnv *, jobject $additionalParamText, j$type newValue) -> void { $`c++Expr`$name = newValue; }
JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_get$name(JNIEnv *, jobject $additionalParamText) -> j$type { return static_cast<j$type> ($`c++Expr`$name); }"""

	fun `c++BooleanArrayAccessor`(name: String, jvmName: String, `c++Name`: String) =
			"""JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_$jvmName(JNIEnv *, jobject, jint index, jboolean newValue) -> void {
	$`c++Expr`$`c++Name`[static_cast<size_t> (index)] = newValue;
}
JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_${jvmName}At(JNIEnv *, jobject, jint index) -> jboolean {
	return static_cast<jboolean> ($`c++Expr`$`c++Name`[static_cast<size_t> (index)] ? JNI_TRUE : JNI_FALSE);
}"""

	fun `c++PrimitiveArrayAccessor`(type: String, name: String, jvmName: String, `c++Name`: String) =
			"""JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_$jvmName(JNIEnv *, jobject, jint index, j$type newValue) -> void {
	$`c++Expr`$`c++Name`[static_cast<size_t> (index)] = newValue;
}
JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_${jvmName}At(JNIEnv *, jobject, jint index) -> j$type {
	return static_cast<j$type> ($`c++Expr`$`c++Name`[static_cast<size_t> (index)]);
}"""

	fun `c++StringedFunction`(name: String, params: List<Param>, type: String?, `c++Expr`: String, init: String = "", deinit: String = "", additionalParamText: String = "") =
			"$JNI_FUNC_PREFIX${className}_$name(JNIEnv *env, jclass ${comma(params)}${
			params.`c++`()}$additionalParamText) -> ${orVoid(type)} {$eol$init ${auto(type)}$`c++Expr`; $deinit ${ret(type, "res", "")} }"
//endregion

	//region Trivial helpers
	fun List<Param>.`c++`() = joinToString { it.`c++`() }

	fun List<Param>.`c++`(builder: StringBuilder) = joinTo(builder) { it.`c++`() }
	fun List<Param>.`c++Expr`() = joinToString { it.`c++Expr`() }
	fun comma(params: List<Param>) = if (params.isNotEmpty()) ", " else ""
	fun boolean(type: String?) = if (type == "boolean") " ? JNI_TRUE : JNI_FALSE" else ""
	fun type(type: String?) = type ?: "void"
	fun ret(type: String?, expr: String = "", orElse: String = expr) = type?.let {
		if (type == "long") "return reinterpret_cast<j$type> ($expr);" else "return static_cast<j$type> ($expr);"
	}
			?: "$orElse;"

	fun auto(type: String?) = type?.let { "auto res = " }.orEmpty()
	fun orVoid(type: String?) = type?.let { "j$it" } ?: "void"
	fun isStatic(params: List<Param>) = params.any { it is StringParam || it is ImVec4Param || it is BoolPtrParam }

	fun genJavaXYAccessor(javaCode: StringBuilder, name: String, type: String) {
		genJavaPrimitiveMember(javaCode, "${name}X", "", type, false, "", name)
		genJavaPrimitiveMember(javaCode, "${name}Y", "", type, false, "", name)
	}

	fun genJavaObjectiveXYAccessor(javaCode: StringBuilder, name: String, type: String) {
		genJavaPrimitiveObjectiveMember(javaCode, type, "${name}X", name)
		genJavaPrimitiveObjectiveMember(javaCode, type, "${name}Y", name)
	}

	fun `c++XYAccessor`(name: String, type: String, additionalParamText: String = "") =
			"""JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_set${name}X(JNIEnv *, jobject $additionalParamText, j$type newValue) -> void { $`c++Expr`$name.x = newValue; }
JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_get${name}X(JNIEnv *, jobject $additionalParamText) -> j$type { return static_cast<j$type> ($`c++Expr`$name.x); }
JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_set${name}Y(JNIEnv *, jobject $additionalParamText, j$type newValue) -> void { $`c++Expr`$name.y = newValue; }
JNIEXPORT auto JNICALL Java_org_ice1000_jimgui_${className}_get${name}Y(JNIEnv *, jobject $additionalParamText) -> j$type { return static_cast<j$type> ($`c++Expr`$name.y); }"""

	fun genJavaFun(javaCode: StringBuilder, function: Fun) = function.let { (name, type, params, visibility, comment) ->
		genJavaFun(javaCode, visibility, params, type, name, comment)
	}

	fun genJavaFun(javaCode: StringBuilder, visibility: String, params: List<Param>, type: String?, name: String, comment: String?) {
		javaCode.javadoc(name.capitalize())

		javaCode.append('\t').append(visibility)
		if (isStatic(params)) {
			javaCode.append(" final ").append(type(type)).append(' ').append(name).append('(')
			params.forEachIndexed { index, param ->
				if (index != 0) javaCode.append(",")
				javaCode.append(param.javaDefault())
			}
			javaCode.append("){")
			if (type != null) javaCode.append("return ")
			javaCode.append(name).append('(')
			params.joinTo(javaCode) { it.javaExpr() }
			javaCode.append(");}").append(eol).append("\tprotected static native ")
		} else javaCode.append(" final native ")
		javaCode.append(type(type)).append(' ').append(name).append('(')
		params.joinTo(javaCode) { it.java() }
		javaCode.append(");").append(eol)
		val defaults = ArrayList<String>(params.size)
		params.asReversed().forEachIndexed inner@{ index, param ->
			val default = param.default?.toString() ?: kotlin.run {
				defaults += param.javaExpr()
				return
			}
			defaults += default
			if (!comment.isNullOrBlank()) javaCode.append("\t/** ").append(comment).appendln(" */")
			javaCode.append("\tpublic ").append(type(type)).append(' ').append(name).append('(')
			val newParams = params.dropLast(index + 1)
			newParams.joinTo(javaCode) { it.javaDefault() }
			javaCode.append("){")
			if (type != null) javaCode.append("return ")
			javaCode.append(name).append('(')
			newParams.joinTo(javaCode) { it.javaExpr() }
			if (newParams.isNotEmpty()) javaCode.append(',')
			defaults.asReversed().joinTo(javaCode)
			javaCode.append(");}").append(eol)
		}
	}

	fun StringBuilder.`c++Expr`(name: String, params: List<Param>, type: String?) =
			append(`c++Expr`).append(name.capitalize()).append('(').append(params.`c++Expr`()).append(')').append(boolean(type))

	fun `genC++Fun`(params: List<Param>, name: String, type: String?, cppCode: StringBuilder, additionalParamText: String = "") {
		val initParams = params.mapNotNull { it.surrounding() }
		if (isStatic(params)) {
			val f = `c++StringedFunction`(name, params, type, "$`c++Expr`${name.capitalize()}(${params.`c++Expr`()})",
					init = initParams.joinToString(" ", prefix = JNI_FUNCTION_INIT) { it.first },
					deinit = initParams.joinToString(" ", postfix = JNI_FUNCTION_CLEAN) { it.second },
					additionalParamText = additionalParamText)
			cppCode.appendln(f)
		} else {
			cppCode.append(JNI_FUNC_PREFIX).append(className).append('_').append(name).append("(JNIEnv *env, jobject")
			if (params.isNotEmpty()) cppCode.append(",")
			cppCode.append(params.`c++`()).append(additionalParamText).append(")->")
			val isVoid = type == null
			if (isVoid) cppCode.append("void")
			else cppCode.append('j').append(type)
			cppCode.appendln('{')
			if (isVoid) cppCode.`c++Expr`(name, params, type).append(';')
			else {
				if (type == "long") cppCode.append("return reinterpret_cast<jlong> (")
				else cppCode.append("return static_cast<j").append(type).append(">(")
				cppCode.`c++Expr`(name, params, type).append(");")
			}
			cppCode.appendln('}')
		}
	}

	fun genJavaBooleanMember(javaCode: StringBuilder, name: String, isArray: Boolean, jvmName: String, annotation: String, `c++Name`: String) {
		javaCode.javadoc(`c++Name`).append("\tpublic native boolean ")
		if (isArray) javaCode.append(jvmName).append("At").append('(').append(annotation).appendln("int index);")
		else javaCode.append("is").append(name).appendln("();")
		javaCode.javadoc(`c++Name`).append("\tpublic native void ")
		if (isArray) javaCode.append(jvmName).append('(').append(annotation).appendln("int index,boolean newValue);")
		else javaCode.append("set").append(name).appendln("(boolean newValue);")
	}

	fun genJavaPrimitiveMember(javaCode: StringBuilder, name: String, annotation: String, type: String, isArray: Boolean, jvmName: String, `c++Name`: String) {
		javaCode.javadoc(`c++Name`).append("\tpublic native ").append(annotation).append(type)
		if (isArray) javaCode.append(' ').append(jvmName).append("At(").append(annotation).appendln("int index);")
		else javaCode.append(" get").append(name).appendln("();")
		javaCode.javadoc(`c++Name`).append("\tpublic native void ")
		if (isArray) javaCode.append(jvmName).append('(').append(annotation).append("int index,")
		else javaCode.append("set").append(name).append('(')
		javaCode.append(annotation).append(type).appendln(" newValue);")
	}

	fun genJavaPrimitiveObjectiveMember(javaCode: StringBuilder, type: String, name: String, `c++Name`: String, ptrName: String = "nativeObjectPtr") {
		javaCode.append("\tprivate native void set").append(name).append("(long ")
				.append(ptrName).append(',').append(type).appendln(" newValue);")
				.javadoc(`c++Name`)
				.append("\tpublic void set").append(name).append('(').append(type).append(" newValue){set")
				.append(name).append('(').append(ptrName).appendln(", newValue); }")
				.append("\tprivate native ").append(type).append(" get")
				.append(name).append("(long ").append(ptrName).appendln(");")
				.javadoc(`c++Name`)
				.append("\tpublic ").append(type).append(" get").append(name)
				.append("(){return get").append(name).append('(').append(ptrName).appendln(");}")
	}

	abstract val `c++Expr`: String
	//endregion
}
