import java.io.File;
import java.util.ArrayList;

import Native.JNINativeMethod;
import Native.NativeArray;
import Native.NativeHelper;
import Native.NativeMethod;
import Smali.SmaliClass;
import Smali.SmaliMethod;
import Smali.SmaliModifier.ModifierAttribute;
import Smali.SmaliModifier.ModifierPremission;
import Smali.SmaliUtils;

public class Main {
	public static void main(String args[]){
		
		//����SmaliΪSmaliClass��
		SmaliClass cls = SmaliUtils.PraseClass(new File(args[0]));
		if(cls == null){
			return;
		}
		SmaliClass.ClassTable.add(cls);
		String methodArrayName = Utils.GetRandomMethodName(0);  //�������
		NativeHelper helper = NativeHelper.getInstance();
		NativeArray<JNINativeMethod> array = new NativeArray<JNINativeMethod>(cls.toString(),methodArrayName);
		ArrayList<SmaliMethod> smaliMethods = cls.getMethods();
		ArrayList<SmaliMethod> addMethod = new ArrayList<SmaliMethod>();
		for(int i = 0 ;i < smaliMethods.size() ; i++){
			SmaliMethod method = smaliMethods.get(i);
			String srcName = method.getMethodName();
			if(method.isNative() || method.isSynthetic()){
				continue;
			}
			//�����������;�̬�����
			if(!"<init>".equals(srcName) && !"<clinit>".equals(srcName) ){
				String reName = Utils.GetRandomMethodName(1);  //�������
				method.rename(reName);
				NativeMethod nativeMethod = NativeMethod.smaliMethod2NativeMethod(method,Utils.GetRandomMethodName(2));  //nativeName�������
				helper.addMethod(nativeMethod);
				array.add(new JNINativeMethod(srcName,method.getMethodSig().toString(),NativeHelper.SmaliType2NativeType(method.getReturnTypeName()),nativeMethod.getMethodName()));
				ArrayList<ModifierAttribute> attributes = method.getAttributes();
				attributes.add(ModifierAttribute.ATTRIBUTE_NATIVE);
				addMethod.add(new SmaliMethod(method.getPremission(),attributes,method.getArgs(),method.getReturnTypeName(),method.getSuperClassName(),srcName,-1));
			}
		}
		for(SmaliMethod method : addMethod){
			cls.addMethod(method, "");
		}
		ArrayList<ModifierAttribute> attr = new ArrayList<ModifierAttribute>();
		attr.add(ModifierAttribute.ATTRIBUTE_CONSTRUCTOR);
		SmaliMethod clinit = new SmaliMethod(ModifierPremission.PREMISSION_DEFAULT,attr,new ArrayList<String>(),"V",cls.toString(),"<clinit>",-1);
		StringBuilder codes = new StringBuilder();
		if(cls.findMethod(clinit) ==  null){
			codes.append("    .locals 1\r\n");
			codes.append("    const-string v0,\"smalisafe\"\r\n");
			codes.append("    invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V\r\n");
			codes.append("    return-void\r\n");
			cls.addMethod(clinit, codes.toString());
		}
		cls.saveChange();
		helper.addJNIMethod(array);
		helper.setOnLoadCode(helper.createOnLoadCode());
		helper.writeSource();
	}
}
