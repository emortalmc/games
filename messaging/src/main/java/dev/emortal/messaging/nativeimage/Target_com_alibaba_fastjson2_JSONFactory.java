package dev.emortal.messaging.nativeimage;

import com.alibaba.fastjson2.introspect.PropertyAccessorFactory;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.alibaba.fastjson2.JSONFactory")
final class Target_com_alibaba_fastjson2_JSONFactory {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static PropertyAccessorFactory PROPERTY_ACCESSOR_FACTORY = new PropertyAccessorFactory();
}
