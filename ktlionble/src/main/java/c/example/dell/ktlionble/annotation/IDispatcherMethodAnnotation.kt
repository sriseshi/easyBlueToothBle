package c.example.dell.ktlionble.annotation

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IDispatcherMethodAnnotation(val CharacteristicUUID: Array<String>, val requestId: Int = -1)