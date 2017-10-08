package com.wstro.rxsubscription.ui;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.wstro.rxsubscription.InjectWriter;
import org.apache.http.util.TextUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * ClassName: InputDialog <br/>
 * Function: TODO ADD FUNCTION. <br/>
 * Reason: TODO ADD REASON. <br/>
 * Date: 2017/10/7 23:27 <br/>
 *
 * @author pengl@wstro
 */
public class InputDialog extends JFrame{
    private JTextField variableEdt;
    private JTextField eventEdt;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel contentPane;
    private JLabel errorLB;

    private PsiClass cls;
    private PsiFile file;
    private Project project;

    private String errorInfo = null;
    private String currentClass = null;

    public InputDialog(PsiClass cls, PsiFile file, Project project) throws HeadlessException {
        this.cls = cls;
        this.file = file;
        this.project = project;
        setContentPane(contentPane);
        setTitle("Add RxSubscription");
        getRootPane().setDefaultButton(okButton);
        this.setAlwaysOnTop(true);
        initGeneratePanel(file);
        initListener();
    }


    private void initListener() {
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        errorLB.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (errorInfo != null) {
                    ErrorDialog errorDialog = new ErrorDialog(errorInfo);
                    errorDialog.setSize(800, 600);
                    errorDialog.setLocationRelativeTo(null);
                    errorDialog.setVisible(true);
                }
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initGeneratePanel(PsiFile file) {

        currentClass = ((PsiJavaFileImpl) file).getPackageName() + "." + file.getName().split("\\.")[0];


    }


    private void onOK() {
        this.setAlwaysOnTop(false);

        String variableName = variableEdt.getText().trim();
        String eventName = eventEdt.getText().trim();

        if (TextUtils.isEmpty(variableName) || TextUtils.isEmpty(eventName)) {
            errorLB.setText("variable name and event name must not empty ");
            return;
        }

        String[] splits = variableName.split(",");
        String[] eventSplits = eventName.split(",");

        if(splits.length != eventSplits.length){
            errorLB.setText("variable name and event name count not equal ");
            return;
        }

        new InjectWriter(file,cls,"Generate Injections",variableName,eventName).execute();

        setVisible(false);
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public void cleanErrorInfo() {
        errorInfo = null;
    }

    public void setErrorInfo(String error) {
        errorInfo = error;
    }

    public void setCls(PsiClass cls) {
        this.cls = cls;
    }

    public void setFile(PsiFile file) {
        this.file = file;
    }

    public void setProject(Project project) {
        this.project = project;
    }


    /*@Override
    public void showError(ConvertBridge.Error err) {
        switch (err) {
            case DATA_ERROR:
                errorLB.setText("data err !!");
                //if (Config.getInstant().isToastError()) {
                    Toast.make(project, errorLB, MessageType.ERROR, "click to see details");
                //}
                break;
            case PARSE_ERROR:
                errorLB.setText("parse err !!");
                if (Config.getInstant().isToastError()) {
                    Toast.make(project, errorLB, MessageType.ERROR, "click to see details");
                }
                break;
            case PATH_ERROR:
                Toast.make(project, generateClassP, MessageType.ERROR, "the path is not allowed");
                break;
        }
    }*/

}
