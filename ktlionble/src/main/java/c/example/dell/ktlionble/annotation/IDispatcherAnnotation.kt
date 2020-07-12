package c.example.dell.ktlionble.annotation


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IDispatcherAnnotation(val CharacteristicUUID: Array<String>)
