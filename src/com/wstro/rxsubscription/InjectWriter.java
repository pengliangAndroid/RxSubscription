package com.wstro.rxsubscription;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.wstro.rxsubscription.utils.PsiClassUtil;

/**
 * ClassName: InjectWriter <br/>
 * Function: TODO ADD FUNCTION. <br/>
 * Reason: TODO ADD REASON. <br/>
 * Date: 2017/10/8 11:47 <br/>
 *
 * @author pengl@wstro
 */
public class InjectWriter extends WriteCommandAction.Simple {
    protected PsiFile mFile;
    protected Project mProject;
    protected PsiClass mClass;
    protected PsiElementFactory mFactory;
    protected String mFieldName;
    protected String mEventName;

    public InjectWriter(PsiFile file, PsiClass clazz, String command, String fieldName, String eventName) {
        super(clazz.getProject(), command);

        mFile = file;
        mProject = clazz.getProject();
        mClass = clazz;
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        this.mFieldName = fieldName;
        this.mEventName = eventName;
    }

    @Override
    protected void run() throws Throwable {
        generateRxSubscription();
        // reformat class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
    }



    private void generateRxSubscription() {
        String[] splits = mFieldName.split(",");
        String[] eventSplits = mEventName.split(",");

        for (int i = 0; i < splits.length; i++) {
            generateField(splits[i]);
            generateBindAndUnbind(splits[i], eventSplits[i]);
        }
    }

    private void  generateField(String filedName){
        mClass.add(mFactory.createFieldFromText(generateFieldText(filedName), mClass));
    }

    private String generateFieldText(String fieldName) {
        StringBuilder fieldSb = new StringBuilder();
        fieldSb.append("private Subscription").append(" ").append(fieldName).append(" ;");
        return fieldSb.append("\n" ).toString();
    }

    private String generateRxText(String fieldName,String eventName) {
        StringBuilder fieldSb = new StringBuilder();
        fieldSb.append(fieldName).append(" = RxBus.getDefault().toObservable(").append(eventName).append(".class)\n")
                .append(".observeOn(AndroidSchedulers.mainThread())\n")
                .append(".subscribe(new Action1<").append(eventName).append(">() {\n")
                .append("@Override\n")
                .append("public void call(").append(eventName).append(" event) {\n")
                .append("\t \n")
                .append("}\n")
                .append("});");
        return fieldSb.append("\n" ).toString();
    }


    protected void generateBindAndUnbind(String fieldName,String eventName) {
       /* PsiClass activityClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Activity", new EverythingGlobalScope(mProject));
        PsiClass fragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Fragment", new EverythingGlobalScope(mProject));
        PsiClass supportFragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.support.v4.app.Fragment", new EverythingGlobalScope(mProject));

        // Check for Activity class
        if (activityClass != null && mClass.isInheritor(activityClass, true)) {
            generateActivityBind(rxText);
            // Check for Fragment class
        } else if ((fragmentClass != null && mClass.isInheritor(fragmentClass, true)) || (supportFragmentClass != null && mClass.isInheritor(supportFragmentClass, true))) {
            generateFragmentBindAndUnbind(rxText);
        }*/

        if(!PsiClassUtil.isClassAvailableForProject(mProject,eventName)){
            PsiDirectory psiDirectory = PsiClassUtil.getCurPackageDir(mFile);
            PsiClass psiClass = JavaDirectoryService.getInstance().createClass(psiDirectory, eventName);
            //FileEditorManager manager = FileEditorManager.getInstance(mProject);
            //manager.openFile(psiClass.getContainingFile().getVirtualFile(), true, true);
        }

        generateBind(fieldName, eventName);
        generateUnbind(fieldName);
    }

    private void generateBind(String fieldName,String eventName) {
        if (mClass.findMethodsByName("initData", false).length == 0) {
            // Add an empty stub of initData()
            StringBuilder method = new StringBuilder();
            method.append("@Override protected void initData() {\n");
            method.append(generateRxText(fieldName,eventName));
            method.append("}\n");

            mClass.add(mFactory.createMethodFromText(method.toString(), mClass));
        } else {
            PsiMethod onCreate = mClass.findMethodsByName("initData", false)[0];
            PsiElement lastBodyElement = onCreate.getBody().getLastBodyElement();
            final PsiStatement initStatement = mFactory.createStatementFromText(generateRxText(fieldName,eventName), mClass);
            onCreate.getBody().addAfter(initStatement, lastBodyElement);
        }
    }

    private void generateUnbind(String fieldName) {
            // Create onDestroyView method if it's missing
        if (mClass.findMethodsByName("onDestroy", false).length == 0) {
            StringBuilder method = new StringBuilder();
            method.append("@Override public void onDestroy() {\n");
            method.append("super.onDestroy();\n");
            method.append(generateUnbindStatement(fieldName));
            method.append("}");

            mClass.add(mFactory.createMethodFromText(method.toString(), mClass));
        } else {
            // there's already onDestroyView(), let's add the unbind statement
            PsiMethod onDestroyView = mClass.findMethodsByName("onDestroy", false)[0];
            StringBuilder unbindText = generateUnbindStatement(fieldName);
            final PsiStatement unbindStatement = mFactory.createStatementFromText(unbindText.toString(), mClass);
            onDestroyView.getBody().addAfter(unbindStatement, onDestroyView.getBody().getLastBodyElement());
        }

    }

    private static StringBuilder generateUnbindStatement(String fieldName) {
        StringBuilder unbindText = new StringBuilder();
        unbindText.append("if (").append(fieldName).append(" != null)\n");
        unbindText.append(fieldName).append(".unsubscribe();\n");
        return unbindText;
    }
}
