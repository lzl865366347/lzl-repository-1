package com.iimsoft.vendor.model;

import com.iimsoft.document.engine.IDocument;
import com.iimsoft.document.engine.IDocumentBL;
import com.iimsoft.i18n.Msg;
import com.iimsoft.util.Services;
import com.iimsoftbase.model.ModelValidationEngine;
import com.iimsoftbase.model.ModelValidator;
import com.iimsoftbase.util.DB;
import com.iimsoftbase.util.TimeUtil;
import com.iimsofttech.ad.dao.IQueryBL;
import com.iimsofttech.model.InterfaceWrapperHelper;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

public class Mvendor extends X_s_vendor implements IDocument{

//    public Mvendor(Properties ctx, int s_vendor_ID, String trxName) {
//        super(ctx, s_vendor_ID, trxName);
//    }


    private static final long serialVersionUID = -2054006884345786340L;
    /** Process Message */
    private String m_processMsg = null;
    /** Just Prepared Flag */
    private boolean m_justPrepared = false;

    public Mvendor(Properties ctx, int s_vendor_ID, String trxName) {
        super(ctx, s_vendor_ID, trxName);
        if (s_vendor_ID == 0) {
            setDateDoc(new Timestamp(System.currentTimeMillis()));
            setDocAction(IDocument.ACTION_Complete); // CO
            setDocStatus(IDocument.STATUS_Drafted); // DR

            setIsApproved(false);
            setProcessed(false);
        }
    }

    public Mvendor(Properties ctx, ResultSet rs, String trxName) {
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
        }
        if (m_processMsg != null)
            return IDocument.STATUS_Invalid;

        // setDefiniteDocumentNo();
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
        if (m_processMsg != null)
            return IDocument.STATUS_Invalid;

        m_justPrepared = true;
        return IDocument.STATUS_InProgress;
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

    /**
     * 业务数据检查
     * @return
     */
    private String checkData(){
        //获取当前对象的Register_ID
        int s_vendorRegister_ID=this.gets_vendorRegister_ID();
        //获取该当前供应商的s_vendorRegiste信息
        I_s_vendorRegister I_s_vendorRegister= InterfaceWrapperHelper.load(s_vendorRegister_ID,I_s_vendorRegister.class);
        String sql="select * " +
                "from  ad_attachment_multiref mul" +
                "join ad_attachmententry ent on mul.ad_attachmententry_id=ent.ad_attachment_id" +
                "left join ad_attachmenttype tp on tp.ad_attachmenttype_id=ent.ad_attachmenttype_id" +
                "join ad_table t on t.ad_table_id=mul.ad_table_id and t.tablename='s_vendor'" +
                "where mul.record_id= '"+ this.gets_vendor_ID()+"'";
        if (I_s_vendorRegister.isrequiredGoodsGTC()==true){


        }


        return null;
    }

}



