package com.iimsoft.vendor.model;

import com.iimsoft.attachments.AttachmentEntryService;
import com.iimsoft.document.engine.IDocument;
import com.iimsoft.document.engine.IDocumentBL;
import com.iimsoft.i18n.Msg;
import com.iimsoft.process.Param;
import com.iimsoft.util.Services;
import com.iimsoftbase.Iimsofttech;
import com.iimsoftbase.model.I_AD_AttachmentEntry;
import com.iimsoftbase.model.ModelValidationEngine;
import com.iimsoftbase.model.ModelValidator;
import com.iimsoftbase.util.DB;
import com.iimsoftbase.util.KeyNamePair;
import com.iimsoftbase.util.TimeUtil;
import com.iimsofttech.model.InterfaceWrapperHelper;
import org.apache.xpath.operations.Bool;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Properties;

public class Msvendor extends X_s_vendor implements IDocument {

    private static final long serialVersionUID = -2054006884345786340L;
    /** Process Message */
    private String m_processMsg = null;
    /** Just Prepared Flag */
    private boolean m_justPrepared = false;

    protected final transient AttachmentEntryService attachmentEntryService = Iimsofttech.getBean(AttachmentEntryService.class);

    @Param(parameterName = I_AD_AttachmentEntry.COLUMNNAME_AD_AttachmentEntry_ID)
    protected int  S_AD_AttachmentEntry_ID;

    public Msvendor(Properties ctx, int s_vendor_ID, String trxName) {
        super(ctx, s_vendor_ID, trxName);
        if (s_vendor_ID == 0) {
            setDateDoc(new Timestamp(System.currentTimeMillis()));
            setDocAction(IDocument.ACTION_Complete); // CO
            setDocStatus(IDocument.STATUS_Drafted); // DR

            setIsApproved(false);
            setProcessed(false);
        }
    }

    public Msvendor(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    public boolean processIt(final String processAction) {
        m_processMsg = null;
        return Services.get(IDocumentBL.class).processIt(this, processAction); // task 09824
    }

    @Override
    public String getProcessMsg() {
        return m_processMsg;
    }

    @Override
    public boolean unlockIt() {
        log.info("unlockIt - " + toString());
        setProcessing(false);
        return true;
    }

    @Override
    public boolean invalidateIt() {
        log.info("invalidateIt - " + toString());
        return true;
    }

    @Override
    public String prepareIt() {
        log.info(toString());
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
        if (m_processMsg != null)
            return IDocument.STATUS_Invalid;

        if(this.getworkflowstatus()==null
                || this.getworkflowstatus().isEmpty()
                || this.getworkflowstatus().equalsIgnoreCase("RD")){
            m_processMsg = checkData();
            if (m_processMsg != null)
                return IDocument.STATUS_Invalid;
        }

        // setDefiniteDocumentNo();
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
        if (m_processMsg != null)
            return IDocument.STATUS_Invalid;

        m_justPrepared = true;
        return IDocument.STATUS_InProgress;
    }
    /**
     * 上传数据检查
     * @return
     */
    private String checkData() {
        //获取当前对象的Register_ID
        int s_vendorRegister_ID=this.gets_vendorRegister_ID();
        //提示信息字符串
        StringBuilder basicStringBuilder = new StringBuilder();
        StringBuilder fileStringBuilder = new StringBuilder();
        if(this.getisAgreeWithOnlineQuotationUndertaking().equals("no")){
            basicStringBuilder.append("AgreeWithOnlineQuotationUndertaking不能为拒绝");
            return basicStringBuilder.toString();
        }
        if (this.getName()==null || this.getName().isEmpty() ){
            basicStringBuilder.append("请填写公司名称");
        }
//        if (this.getStatus()==null || this.getStatus().isEmpty() ){
//            Basic.append("请填写公司地址");
//        }
        if(this.getofficeMailingAddr()==null){
            basicStringBuilder.append("请填写公司邮寄地址");
        }
       if (this.getregisteredAddr()==null){
           basicStringBuilder.append("请填写公司注册地址");
       }
       if(this.getCountry()<1){
           basicStringBuilder.append("请填写国家");
       }
       if (this.getCountry()==153){
           if (this.getRegion()==null || this.getRegion().isEmpty()){
               basicStringBuilder.append("国家为中国时必填地区");
           }
       }
       if(this.getcompanyPresidentOrLegalRepresentative()==null || this.getcompanyPresidentOrLegalRepresentative().isEmpty()){
           basicStringBuilder.append("请填写公司法人信息");
       }
       if(this.getregisteredCapital().equals(BigDecimal.ZERO)){
           basicStringBuilder.append("请填写公司注册资本");
       }
       if(this.getcurrencyUnit()<1){
           basicStringBuilder.append("请填写货币单位");
       }
       if(this.getcompanyRegistrationDate()==null){
           basicStringBuilder.append("请填写公司注册日期");
       }
       if(this.getppTotalEmployees()==0){
           basicStringBuilder.append("请填写公司员工人数");
       }
       if (this.getbusinessScope()==null || this.getbusinessScope().isEmpty()){
           basicStringBuilder.append("请填写供应范围");
       }
       if (this.getshareholders()==null || this.getshareholders().isEmpty()){
           basicStringBuilder.append("请填写股东");
       }
       if(this.getmainContactName()==null || this.getmainContactName().isEmpty()){
           basicStringBuilder.append("请填供应商主联系人");
       }

        //获取该当前供应商的s_vendorRegiste信息
        I_s_vendorRegister vendorRegisterModel= InterfaceWrapperHelper.load(s_vendorRegister_ID,I_s_vendorRegister.class);
        String sql="select tp.ad_attachmenttype_id,tp.name " +
                   "from  ad_attachment_multiref mul " +
                   "join ad_attachmententry ent on mul.ad_attachmententry_id=ent.ad_attachmententry_id " +
                   "left join ad_attachmenttype tp on tp.ad_attachmenttype_id=ent.ad_attachmenttype_id " +
                   "join ad_table t on t.ad_table_id=mul.ad_table_id and t.tablename='s_vendor' " +
                   "where mul.record_id= '"+ this.gets_vendor_ID()+"'";
        //查询供应商已经上传的文件种类
        KeyNamePair[] data = DB.getKeyNamePairs(sql,true);
            if (!fileJudge("公司简介",data)){
                fileStringBuilder.append("未上传公司简介文件") ;
            }

            if (this.getisAgreeWithGTC()=="yes"){
                if (!fileJudge("GTC盖章扫描件",data)){
                    fileStringBuilder.append("未上传GTC盖章扫描件") ;
                }
            }
            if (this.getisAgreeWithGTC()=="deviate"){
                if (!fileJudge("GTC偏离表",data)){
                    fileStringBuilder.append("未上传GTC偏离表") ;
                }
            }
        if (this.getisAgreeWithNDA()=="yes"){
            if (!fileJudge("NDA盖章扫描件",data)){
                fileStringBuilder.append("未上传NDA盖章扫描件") ;
            }
        }
        if (this.getisAgreeWithNDA()=="deviate"){
            if (!fileJudge("NDA偏离表",data)){
                fileStringBuilder.append("NDA偏离表") ;
            }
        }
            if (vendorRegisterModel.isrequiredServiceGTC()) {
                if (!fileJudge("GTC服务采购",data))
                fileStringBuilder.append("未上传GTC服务采购文件") ;
            }
            if (vendorRegisterModel.isrequiredGoodsGTC()) {
                if (!fileJudge("GTC货物采购",data))
                    fileStringBuilder.append("未上传GTC货物采购文件") ;
            }
            if (vendorRegisterModel.isrequiredServiceGTCDeviation()) {
                if (!fileJudge("GTC偏离表_货物采购",data))
                    fileStringBuilder.append("未上传GTC偏离表_货物采购文件") ;
            }
            if (vendorRegisterModel.isrequiredGoodsGTCDeviation()) {
                if (!fileJudge("GTC偏离表_服务采购",data))
                    fileStringBuilder.append("未上传GTC偏离表_服务采购文件");
            }
            if (vendorRegisterModel.isrequiredSingleGuaranteeNDA()) {
                if (!fileJudge("NDA_单方保密",data))
                    fileStringBuilder.append("未上传NDA_单方保密文件") ;
            }
            if (vendorRegisterModel.isrequiredMutualGuaranteeNDA()) {
                if (!fileJudge("NDA_互负保密",data))
                    fileStringBuilder.append("未上传NDA_互负保密文件");
            }
            if (vendorRegisterModel.isrequiredOnlineQuotationUndertaking()) {
                if (!fileJudge("门户系统使用须知",data))
                    fileStringBuilder.append("未上传门户系统使用须知文件");
            }
            if (vendorRegisterModel.isrequiredBusinessLicense()) {
                if (!fileJudge("营业执照",data))
                    fileStringBuilder.append("未上传营业执照文件");
            }
            if (vendorRegisterModel.isrequiredCertifications()) {
                if (!fileJudge("资质证书",data))
                    fileStringBuilder.append("未上传资质证书文件");
            }
            if (vendorRegisterModel.isrequiredAgentAuthorizationCertificate()) {
                if (!fileJudge("授权代理证书",data))
                    fileStringBuilder.append("未上传授权代理证书文件");
            }
            if (vendorRegisterModel.isrequiredSuccessfulCasesInRecentYears()) {
                if (!fileJudge("近年成功业绩",data))
                    fileStringBuilder.append("未上传近年成功业绩文件");
            }
            if (vendorRegisterModel.isrequiredSalesInRecentYears()) {
                if (!fileJudge("近年销售额",data))
                    fileStringBuilder.append("未上传近年销售额文件");
            }
            if (vendorRegisterModel.isauditReport()) {
                if (!fileJudge("审计报告",data))
                    fileStringBuilder.append("未上传审计报告文件");
            }
        String Files = basicStringBuilder.toString() + fileStringBuilder.toString();
        return Files.isEmpty() ? null : Files;
    }

    //判断是否上传文件
    public Boolean fileJudge(String name, KeyNamePair[] data){
        boolean matched = false;
        for (KeyNamePair datum : data) {
            if(datum.getName().equals(name)){
                matched = true;
                break;
            }
        }
        return matched;
    }
    @Override
    public boolean approveIt() {
        setIsApproved(true);
        return true;
    }

    @Override
    public boolean rejectIt() {
        log.info("rejectIt - " + toString());
        setIsApproved(false);
        return true;
    }

    @Override
    public String completeIt() {
        // Re-Check
        if (!m_justPrepared) {
            String status = prepareIt();
            if (!IDocument.STATUS_InProgress.equals(status))
                return status;
        }

        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
        if (m_processMsg != null)
            return IDocument.STATUS_Invalid;

        // Implicit Approval
        if (!isApproved())
            approveIt();
        log.debug("Completed: {}", this);

        // User Validation
        String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
        if (valid != null) {
            m_processMsg = valid;
            return IDocument.STATUS_Invalid;
        }

        // Set the definite document number after completed (if needed)
        // setDefiniteDocumentNo();
        setProcessed(true);
        setDocAction(ACTION_Close);
        return IDocument.STATUS_Completed;

    }

    @Override
    public boolean voidIt() {
        log.info("voidIt - " + toString());
        // Before Void
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_VOID);
        if (m_processMsg != null)
            return false;

        if (!closeIt())
            return false;

        // After Void
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
        if (m_processMsg != null)
            return false;

        return true;
    }

    @Override
    public boolean closeIt() {
        log.info("closeIt - " + toString());
        // Before Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_CLOSE);
        if (m_processMsg != null)
            return false;

        // After Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_CLOSE);
        if (m_processMsg != null)
            return false;

        return true;

    }

    @Override
    public boolean reverseCorrectIt() {
        log.info("reverseCorrectIt - " + toString());
        // Before reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        // After reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        return false;

    }

    @Override
    public boolean reverseAccrualIt() {
        log.info("reverseAccrualIt - " + toString());
        // Before reverseAccrual
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
        if (m_processMsg != null)
            return false;

        // After reverseAccrual
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
        if (m_processMsg != null)
            return false;

        return false;

    }

    @Override
    public boolean reActivateIt() {
        log.info("reActivateIt - " + toString());
        // Before reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);
        if (m_processMsg != null)
            return false;

        // setProcessed(false);
        if (!reverseCorrectIt())
            return false;

        // After reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
        if (m_processMsg != null)
            return false;

        return true;
    }

    @Override
    public File createPDF() {
        try {
            File temp = File.createTempFile(get_TableName() + get_ID() + "_", ".pdf");
            return createPDF(temp);
        } catch (Exception e) {
            log.error("Could not create PDF - " + e.getMessage());
        }
        return null;
    }

    /**
     * Create PDF file
     *
     * @param file
     *            output file
     * @return file if success
     */
    public File createPDF(File file) {
        // ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.INVOICE,
        // getC_Invoice_ID());
        // if (re == null)
        return null;
        // return re.getPDF(file);
    } // createPDF

    @Override
    public String getSummary() {
        StringBuffer sb = new StringBuffer();
        sb.append(getDocumentNo());

        if (getDescription() != null && getDescription().length() > 0)
            sb.append(" - ").append(getDescription());
        return sb.toString();
    }

    @Override
    public String getDocumentInfo() {
        return Msg.getElement(getCtx(), "s_vendor_ID") + " " + getDocumentNo();
    }

    @Override
    public int getDoc_User_ID() {
        return getAD_User_ID();
    }

    @Override
    public int getC_Currency_ID() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public BigDecimal getApprovalAmt() {
        // TODO Auto-generated method stub
        return new BigDecimal(0);
    }

    @Override
    public LocalDate getDocumentDate() {
        return TimeUtil.asLocalDate(getDateDoc());
    }



}
