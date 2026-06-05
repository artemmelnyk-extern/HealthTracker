# Keep Firestore DTO classes — R8 strips no-arg constructors needed for deserialization
-keep class com.genegebra.healthtracker.data.model.** { *; }
-keep class com.genegebra.healthtracker.domain.model.** { *; }
