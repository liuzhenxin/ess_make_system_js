package com.clt.ess.web;

import com.clt.ess.base.Constant;
import com.clt.ess.bean.ResultMessageBeen;
import com.clt.ess.dao.IPersonDao;
import com.clt.ess.entity.*;
import com.clt.ess.service.*;
import com.clt.ess.utils.FastJsonUtil;
import com.clt.ess.utils.ReadPfx;
import com.clt.ess.utils.StringUtils;
import com.multica.crypt.MuticaCrypt;
import com.multica.crypt.MuticaCryptException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.clt.ess.utils.Base64Utils.ESSGetBase64Decode;
import static com.clt.ess.utils.FileUtil.byte2File;
import static com.clt.ess.utils.FileUtil.saveFile;
import static com.clt.ess.utils.PFXUtil.getX509Certificate;
import static com.clt.ess.utils.PowerUtil.getLoginUser;
import static com.clt.ess.utils.StringUtils.isNull;
import static com.clt.ess.utils.dateUtil.getDate;
import static com.clt.ess.utils.uuidUtil.getEssUUID;
import static com.clt.ess.utils.uuidUtil.getUUID;
import static com.multica.crypt.MuticaCrypt.ESSGetBase64Encode;
import static java.lang.Thread.sleep;

@Controller
@RequestMapping("/make")
public class MakeController {

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected HttpSession session;

    @Autowired
    private ISealService sealService;
    @Autowired
    private IUnitService unitService;
    @Autowired
    private ICertificateService certificateService;
    @Autowired
    private IMessageService messageService;
    @Autowired
    private ILogService logService;
    @Autowired
    private IFileTypeService fileTypeService;
    @Autowired
    private ISealImgService sealImgService;
    @Autowired
    private IErrorLogService errorLogService;
    @Autowired
    private IPersonDao personDao;
    /**
     * 每次拦截到请求会先访问此函数
     * @param request http请求
     * @param response http回应
     */
    @ModelAttribute
    public void setReqAndRes(HttpServletRequest request, HttpServletResponse response){
        this.request = request;
        this.response = response;
        this.session = request.getSession();

    }

    /**
     *访问印章制作页面
     * @param unitId 单位ID
     */
    @RequestMapping(value="/list.html", method = RequestMethod.GET)
    public String seal_make(Model model, String unitId) {

        SealApply sealApply = new SealApply();
        sealApply.setUnitId(unitId);
        //审核通过
        sealApply.setApplyState(Constant.REVIEW_THROUGH);
        List<SealApply> sealApplyList =  sealService.findSealApply(sealApply);
        //制作失败
        sealApply.setApplyState(Constant.MAKE_NO_COMPLETION);
        List<SealApply> sealApplyList_1 =  sealService.findSealApply(sealApply);
        sealApplyList.addAll(sealApplyList_1);

        model.addAttribute("unit",  unitService.findUnitById(unitId));
        model.addAttribute("sealApplyList",  sealApplyList);

        return "make/make_list";

    }
    /**
     *访问印章制作
     * @param sealApplyId 申请信息ID
     */
    @RequestMapping(value="/make_detail.html", method = RequestMethod.GET)
    public ModelAndView seal_make_detail(String sealApplyId) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        //根据申请信息ID获取详细信息
        SealApply sealApply = sealService.findSealApplyById(sealApplyId);
        if(sealApply==null){
            modelAndView.setViewName("error");
        }
        //查询当前单位对应的顶级单位支持的授权类型
        List<FileType> fileTypeList = fileTypeService.findFileTypeListByTop(sealApply.getUnitId());
        modelAndView.addObject("fileTypeList",  fileTypeList);
        modelAndView.addObject("sealApply",sealApply);
        //查询当前单位对应的顶级单位支持的证书颁发机构
        Unit unit = unitService.findTopUnit(sealApply.getUnitId());
        if(!"".equals(sealApply.getCerIssuer())){
            //获取证书颁发机构
            List<IssuerUnit> issuerUnitList = unitService.findIssuerUnitByUnitId(unit.getUnitId());
            modelAndView.addObject("issuerUnitList", issuerUnitList);
        }
        //判断申请信息类别
        switch(sealApply.getApplyType()){
            //申请新印章
            case Constant.APPLYTYPE_NEW:
                modelAndView.setViewName("make/make_detail");
                break;
            //注册UK
            case Constant.APPLYTYPE_REGISTER_UK:
                modelAndView.setViewName("make/make_detail_uk");
                break;
            //印章重做
            case Constant.APPLYTYPE_REPEAT:
                modelAndView.setViewName("make/make_detail_repeat");
                break;
            //印章授权延期
            case Constant.APPLYTYPE_DELAY_AUTH:
                modelAndView.setViewName("make/make_detail_delay");
                break;
            //证书授权延期
            case Constant.APPLYTYPE_DELAY_CER:
                modelAndView.setViewName("make/make_detail_delay_cer");
                break;
            default:
                break;
        }
        return modelAndView;

    }
    /**
     *访问印章制作
     */
    @RequestMapping(value="/make_new_do.html", method = RequestMethod.POST,produces="text/html;charset=UTF-8")
    @ResponseBody
    public String make_new_do(String sealApplyId,String cerIssuer,String cardType,Certificate c, MultipartFile gifImg,
                               MultipartFile jpgImg) {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        //查找当前操作的申请信息
        SealApply sealApply = sealService.findSealApplyById(sealApplyId);
        //UK类型
        sealApply.setCardType(cardType);
        //根据上传的图像生成印章图片
        SealImg sealImg = new SealImg();
        sealImg.setSealImgId(getEssUUID(sealApply.getUnitId()));
        try {
            if(jpgImg!=null|| gifImg!=null){
                //如果提供了jpg图片，将jpg图片写入缩略图
                if (jpgImg!=null) {

                    sealImg.setSealImgJpg(jpgImg.getBytes());

                    sealImg.setSealThumbnailImgBase64(ESSGetBase64Encode(jpgImg.getBytes()));
                    sealImg.setSealImageType("jpg");
                }
                if (gifImg!=null) {
                    sealImg.setSealImgGifBase64(ESSGetBase64Encode(gifImg.getBytes()));
                    sealImg.setSealImageType("gif");
                }
                //如果只提供了gif图片，将gif图片写入缩略图
                if(gifImg!=null &&jpgImg==null){
                    sealImg.setSealThumbnailImgBase64(ESSGetBase64Encode(gifImg.getBytes()));
                }
                if(gifImg!=null &&jpgImg!=null){
                    sealImg.setSealImageType("gif@jpg");
                }
            }else{
                messageBeen.setMessage("error");
                messageBeen.setBody("图片不可为空！");
            }
        } catch (IOException e) {
            messageBeen.setMessage("error");
            messageBeen.setBody("图片处理出现错误！");
        }
        //获取印章图片ID
        String sealImgId = sealImgService.addSealImg(sealImg);
        //添加证书ID
        sealApply.setSealImgId(sealImgId);

        //状态修改为制作完成
        sealApply.setApplyState(Constant.MAKE_COMPLETION);
        //设置制作人和制作时间
        sealApply.setMakeUserId((getLoginUser(session).getUserId()));
        sealApply.setMakeTime(getDate());


        //设置证书颁发单位 这里默认一个值 江苏省烟草公司电子印章平台
        sealApply.setCerIssuer(cerIssuer);

        //证书相关的处理
        Certificate certificate =sealApply.getCertificate();

        certificate.setCountry(c.getCountry());
        certificate.setProvince(c.getProvince());
        certificate.setCity(c.getCity());
        certificate.setCertUnit(c.getCertUnit());
        certificate.setCertDepartment(c.getCertDepartment());
        certificate.setCerName(c.getCerName());

        //判断是否手签
        if(sealApply.getSealTypeId().contains("st7")){
            //设置证书的颁发者单位
            certificate.setIssuerUnitId(sealApply.getCerIssuer());
            //手签 自己生成证书 证书起始时间 根据印章起始时间决定
            certificate.setStartTime(sealApply.getSealStartTime());
            certificate.setEndTime(sealApply.getSealEndTime());

            certificate.setCerPsw(StringUtils.getESSPwd());
            certificate.setFileState(Constant.FILE_STATE_CERANDPFX);
            //生成证书
            Map<String, String> cerAndPfxMap =  certificateService.createCerFileAndPfx(certificate);

            certificate.setPfxBase64(cerAndPfxMap.get("pfxBase64"));
            certificate.setCerBase64(cerAndPfxMap.get("cerBase64"));
            //生成证书hash
            byte[] essGetDigest ;
            try {
                essGetDigest = MuticaCrypt.ESSGetDigest(ESSGetBase64Decode(cerAndPfxMap.get("cerBase64")));
            } catch (MuticaCryptException e) {
                errorLogService.addErrorLog("ApplyController-ESSGetDigest-证书生成错误-null！");
                messageBeen.setMessage("error");
                messageBeen.setBody("证书生成错误，稍后再试！");
                return FastJsonUtil.toJSONString(messageBeen);
            }
            String certHash = MuticaCrypt.ESSGetBase64Encode(essGetDigest);
            certificate.setCertHash(certHash);

            //在这里更新个人的证书
            Person p =new Person();
            p.setIdNum(sealApply.getSealHwUserIdNum());
            p.setCerId(certificate.getCertificateId());
            personDao.updatePersonByIDNum(p);

        }else{
            certificate.setCerPsw(c.getCerPsw());
            certificate.setFileState(Constant.FILE_STATE_CERANDPFX);
            //公章
            //解析pfx证书的信息
            Map<String,String> cerInfoMap = new HashMap<>();
            try {
                //解析pfx证书的信息
                cerInfoMap = ReadPfx.GetCertInfoFromPfxBase64(c.getPfxBase64(),c.getCerPsw());
                String algorithm = cerInfoMap.get("algorithm");
                String issuer = cerInfoMap.get("issuer");
                String endTime = cerInfoMap.get("endTime");
                String startTime = cerInfoMap.get("startTime");
                String version = cerInfoMap.get("version");
                certificate.setAlgorithm(algorithm);
                certificate.setIssuer(issuer);
                certificate.setStartTime(startTime);
                certificate.setEndTime(endTime);
                certificate.setCertificateVersion(version);
                //解析出cer证书 存入
                X509Certificate certificateObject = getX509Certificate(ESSGetBase64Decode(c.getPfxBase64()),c.getCerPsw());
                //生成hash数据
                byte[] essGetDigest = MuticaCrypt.ESSGetDigest(certificateObject.getEncoded());
                //编码hash
                String certHash = MuticaCrypt.ESSGetBase64Encode(essGetDigest);
                String certBase64 = ESSGetBase64Encode(certificateObject.getEncoded());
                certificate.setPfxBase64(c.getPfxBase64());
                certificate.setCerBase64(certBase64);
                certificate.setCertHash(certHash);

            } catch (Exception e) {
                errorLogService.addErrorLog("ApplyController-getPfxInfo-解析pfx证书出错-null！");
                messageBeen.setMessage("error");
                messageBeen.setBody("证书解析错误，请检查密码！");
                return FastJsonUtil.toJSONString(messageBeen);
            }
        }
        //根据已有信息生成印章对象
        Seal newSeal = new Seal();
        //印章Id
        newSeal.setSealId(sealApply.getSealId());
        //印章名称
        newSeal.setSealName(sealApply.getSealName());
        //印章图像
        newSeal.setSealImgId(sealApply.getSealImgId());
        //印章证书
        newSeal.setCerId(sealApply.getCertificateId());
        //制作时间
        newSeal.setInputTime(sealApply.getMakeTime());
        //制作人
        newSeal.setInputUserId(sealApply.getMakeUserId());
        //印章单位
        newSeal.setUnitId(sealApply.getUnitId());
        //印章类型
        newSeal.setSealTypeId(sealApply.getSealTypeId());
        //手签身份证号
        newSeal.setSealHwIdNum(sealApply.getSealHwUserIdNum());
        //印章授权
        newSeal.setFileTypeNum(sealApply.getFileTypeNum());
        //印章授权时间
        newSeal.setSealStartTime(sealApply.getSealStartTime());
        newSeal.setSealEndTime(sealApply.getSealEndTime());
        //UK上的授权时间和信息
        newSeal.setAuthorizationTime(sealApply.getAuthorizationTime());
        newSeal.setAuthorizationInfo(sealApply.getAuthorizationInfo());
        //keyId
        newSeal.setKeyId(sealApply.getKeyId());
        //是否UK印章
        newSeal.setIsUK(sealApply.getIsUK());
        //印章状态有效
        newSeal.setSealState(Constant.SEAL_STATE_VALID);
        newSeal.setCardType(sealApply.getCardType());

        if("success".equals(messageBeen.getMessage())){
            try {
                sealService.addSeal(newSeal);
                certificateService.updateCertificate(certificate);
                sealService.updateSealApply(sealApply);
                messageBeen.setBody(newSeal);
                logService.addSystemLog("制作了"+newSeal.getSealName(),"印章制作",
                        newSeal.getUnitId(),getLoginUser(session).getUserId(),"");
            }catch (Exception e){
                messageBeen.setMessage("error");
                messageBeen.setBody("数据更新时出现错误！");
            }
        }
        return FastJsonUtil.toJSONString(messageBeen);
    }

    /**
     *注册UK制作
     * @param sealApplyId
     */
    @RequestMapping(value="/make_uk_do.html", method = RequestMethod.POST)
    @ResponseBody
    public String make_uk_do(String sealApplyId,MultipartFile gifImg,MultipartFile jpgImg) throws IOException {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        //查找当前操作的申请信息
        SealApply sealApply = sealService.findSealApplyById(sealApplyId);
        //根据上传的图像生成印章图片
        SealImg sealImg = new SealImg();
        sealImg.setSealImgId(getEssUUID(sealApply.getUnitId()));
        if(jpgImg!=null|| gifImg!=null){
            //如果提供了jpg图片，将jpg图片写入缩略图
            if (jpgImg!=null) {
                sealImg.setSealImgJpg(jpgImg.getBytes());
                sealImg.setSealThumbnailImgBase64(ESSGetBase64Encode(jpgImg.getBytes()));
            }
            if (gifImg!=null) {
                sealImg.setSealImgGifBase64(ESSGetBase64Encode(gifImg.getBytes()));
            }
            //如果只提供了gif图片，将gif图片写入缩略图
            if(gifImg!=null &&jpgImg==null){
                sealImg.setSealThumbnailImgBase64(ESSGetBase64Encode(gifImg.getBytes()));
            }
        }else{
            messageBeen.setMessage("error");
        }
        //获取印章图片ID
        String sealImgId = sealImgService.addSealImg(sealImg);
        //添加图片ID
        sealApply.setSealImgId(sealImgId);
//        //判断当前是否有印章余额 结果为3 时 允许添加
//        if(sealService.isAddSeal(sealApply.getUnitId())!=3){
//            messageBeen.setMessage("error");
//        }
        User user =  getLoginUser(session);
        //状态修改为制作完成
        sealApply.setApplyState(Constant.MAKE_COMPLETION);
        //设置制作人和制作时间
        sealApply.setMakeUserId((user.getUserId()));
        sealApply.setMakeTime(getDate());

        //根据已有信息生成印章对象
        Seal newSeal = new Seal();
        //印章Id
        newSeal.setSealId(sealApply.getSealId());
        //印章名称
        newSeal.setSealName(sealApply.getSealName());
        //印章图像
        newSeal.setSealImgId(sealApply.getSealImgId());
        //印章证书
        newSeal.setCerId(sealApply.getCertificateId());
        //制作时间
        newSeal.setInputTime(sealApply.getMakeTime());
        //制作人
        newSeal.setInputUserId(sealApply.getMakeUserId());
        //印章单位
        newSeal.setUnitId(sealApply.getUnitId());
        //印章类型
        newSeal.setSealTypeId(sealApply.getSealTypeId());
        //手签身份证号
        newSeal.setSealHwIdNum(sealApply.getSealHwUserIdNum());
        //印章授权
        newSeal.setFileTypeNum(sealApply.getFileTypeNum());
        //印章授权时间
        newSeal.setSealStartTime(sealApply.getSealStartTime());
        newSeal.setSealEndTime(sealApply.getSealEndTime());
        //UK上的授权时间和信息
        newSeal.setAuthorizationTime(sealApply.getAuthorizationTime());
        newSeal.setAuthorizationInfo(sealApply.getAuthorizationInfo());
        //keyId
        newSeal.setKeyId(sealApply.getKeyId());
        //是否UK印章
        newSeal.setIsUK(sealApply.getIsUK());
        //印章状态无效
        newSeal.setSealState(Constant.SEAL_STATE_INVALID);
        newSeal.setCardType(sealApply.getCardType());

        if("success".equals(messageBeen.getMessage())){
            try {
                sealService.addSeal(newSeal);
                sealService.updateSealApply(sealApply);
                messageBeen.setBody(newSeal);

                //向消息中心发消息
                Message message_new  = new Message();
                message_new.setMessageNo(getEssUUID(newSeal.getUnitId()));
                message_new.setSender(user.getUserId());
                message_new.setReceiver(sealApply.getApplyUserId());
                message_new.setSendTime(getDate());
                message_new.setMessageType(Constant.Message_Type_register);
                message_new.setMessageTitle("注册成功,请确认！");
                message_new.setMessageContent(sealApply.getSealName()+"UK注册已审核制作完成，请确认！");
                message_new.setApplyInfoId(sealApply.getSealApplyId());
                message_new.setReadState(2);
                message_new.setState(1);
                messageService.addMessage(message_new);

                logService.addSystemLog("制作了"+newSeal.getSealName(),"印章注册",
                        newSeal.getUnitId(),user.getUserId(),"");
            }catch (Exception e){
                messageBeen.setMessage("error");
            }
        }
        return new String(FastJsonUtil.toJSONString(messageBeen).getBytes("utf8"),"iso8859-1");
    }


    /**
     *重做
     * @param sealApplyId
     */
    @RequestMapping(value="/make_repeat_do.html", method = RequestMethod.POST,produces="text/html;charset=UTF-8")
    @ResponseBody
    public String make_repeat_do(String sealApplyId,String cerIssuer,String cardType,Certificate c, MultipartFile gifImg,
                              MultipartFile jpgImg) throws IOException {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        //查找当前操作的申请信息
        SealApply sealApply = sealService.findSealApplyById(sealApplyId);
        //UK类型
        sealApply.setCardType(cardType);
        //根据上传的图像生成印章图片
        SealImg sealImg = new SealImg();
        sealImg.setSealImgId(getEssUUID(sealApply.getUnitId()));
        try {
            if(jpgImg!=null|| gifImg!=null){
                //如果提供了jpg图片，将jpg图片写入缩略图
                if (jpgImg!=null) {

                    sealImg.setSealImgJpg(jpgImg.getBytes());

                    sealImg.setSealThumbnailImgBase64(ESSGetBase64Encode(jpgImg.getBytes()));
                    sealImg.setSealImageType("jpg");
                }
                if (gifImg!=null) {
                    sealImg.setSealImgGifBase64(ESSGetBase64Encode(gifImg.getBytes()));
                    sealImg.setSealImageType("gif");
                }
                //如果只提供了gif图片，将gif图片写入缩略图
                if(gifImg!=null &&jpgImg==null){
                    sealImg.setSealThumbnailImgBase64(ESSGetBase64Encode(gifImg.getBytes()));
                }
                if(gifImg!=null &&jpgImg!=null){
                    sealImg.setSealImageType("gif@jpg");
                }
            }else{
                messageBeen.setMessage("error");
                messageBeen.setBody("图片不可为空！");
            }
        } catch (IOException e) {
            messageBeen.setMessage("error");
            messageBeen.setBody("图片处理出现错误！");
        }
        //获取印章图片ID
        String sealImgId = sealImgService.addSealImg(sealImg);
        //添加证书ID
        sealApply.setSealImgId(sealImgId);

        //状态修改为制作完成
        sealApply.setApplyState(Constant.MAKE_COMPLETION);
        //设置制作人和制作时间
        sealApply.setMakeUserId((getLoginUser(session).getUserId()));
        sealApply.setMakeTime(getDate());

        //设置证书颁发单位
        sealApply.setCerIssuer(cerIssuer);

        //证书相关的处理
        Certificate certificate =sealApply.getCertificate();

        certificate.setCountry(c.getCountry());
        certificate.setProvince(c.getProvince());
        certificate.setCity(c.getCity());
        certificate.setCertUnit(c.getCertUnit());
        certificate.setCertDepartment(c.getCertDepartment());
        certificate.setCerName(c.getCerName());
        //判断是否手签
        if(sealApply.getSealTypeId().contains("st7")){
            //设置证书的颁发者单位
            certificate.setIssuerUnitId(sealApply.getCerIssuer());
            //手签 自己生成证书 证书起始时间 根据印章起始时间决定
            certificate.setStartTime(sealApply.getSealStartTime());
            certificate.setEndTime(sealApply.getSealEndTime());

            certificate.setCerPsw(StringUtils.getESSPwd());
            certificate.setFileState(Constant.FILE_STATE_CERANDPFX);
            //生成证书
            Map<String, String> cerAndPfxMap =  certificateService.createCerFileAndPfx(certificate);

            certificate.setPfxBase64(cerAndPfxMap.get("pfxBase64"));
            certificate.setCerBase64(cerAndPfxMap.get("cerBase64"));
            //生成证书hash
            byte[] essGetDigest ;
            try {
                essGetDigest = MuticaCrypt.ESSGetDigest(ESSGetBase64Decode(cerAndPfxMap.get("cerBase64")));
            } catch (MuticaCryptException e) {
                errorLogService.addErrorLog("ApplyController-ESSGetDigest-证书生成错误-null！");
                messageBeen.setMessage("error");
                messageBeen.setBody("证书生成错误，稍后再试！");
                return FastJsonUtil.toJSONString(messageBeen);
            }
            String certHash = MuticaCrypt.ESSGetBase64Encode(essGetDigest);
            certificate.setCertHash(certHash);

            //在这里更新个人的证书
            Person p =new Person();
            p.setIdNum(sealApply.getSealHwUserIdNum());
            p.setCerId(certificate.getCertificateId());
            personDao.updatePersonByIDNum(p);

        }else{
            certificate.setCerPsw(c.getCerPsw());
            certificate.setFileState(Constant.FILE_STATE_CERANDPFX);
            //公章
            //解析pfx证书的信息
            Map<String,String> cerInfoMap = new HashMap<>();
            try {
                //解析pfx证书的信息
                cerInfoMap = ReadPfx.GetCertInfoFromPfxBase64(c.getPfxBase64(),c.getCerPsw());
                String algorithm = cerInfoMap.get("algorithm");
                String issuer = cerInfoMap.get("issuer");
                String endTime = cerInfoMap.get("endTime");
                String startTime = cerInfoMap.get("startTime");
                String version = cerInfoMap.get("version");
                certificate.setAlgorithm(algorithm);
                certificate.setIssuer(issuer);
                certificate.setStartTime(startTime);
                certificate.setEndTime(endTime);
                certificate.setCertificateVersion(version);
                //解析出cer证书 存入
                X509Certificate certificateObject = getX509Certificate(ESSGetBase64Decode(c.getPfxBase64()),c.getCerPsw());
                //生成hash数据
                byte[] essGetDigest = MuticaCrypt.ESSGetDigest(certificateObject.getEncoded());
                //编码hash
                String certHash = MuticaCrypt.ESSGetBase64Encode(essGetDigest);
                String certBase64 = ESSGetBase64Encode(certificateObject.getEncoded());
                certificate.setPfxBase64(c.getPfxBase64());
                certificate.setCerBase64(certBase64);
                certificate.setCertHash(certHash);

            } catch (Exception e) {
                errorLogService.addErrorLog("ApplyController-getPfxInfo-解析pfx证书出错-null！");
                messageBeen.setMessage("error");
                messageBeen.setBody("证书解析错误，请检查密码！");
                return FastJsonUtil.toJSONString(messageBeen);
            }
        }
        //根据已有信息生成印章对象
        Seal newSeal = new Seal();
        //印章Id
        newSeal.setSealId(sealApply.getSealId());
        //印章名称
        newSeal.setSealName(sealApply.getSealName());
        //印章图像
        newSeal.setSealImgId(sealApply.getSealImgId());
        //印章证书
        newSeal.setCerId(sealApply.getCertificateId());
        //制作时间
        newSeal.setInputTime(sealApply.getMakeTime());
        //制作人
        newSeal.setInputUserId(sealApply.getMakeUserId());
        //印章单位
        newSeal.setUnitId(sealApply.getUnitId());
        //印章类型
        newSeal.setSealTypeId(sealApply.getSealTypeId());
        //手签身份证号
        newSeal.setSealHwIdNum(sealApply.getSealHwUserIdNum());
        //印章授权
        newSeal.setFileTypeNum(sealApply.getFileTypeNum());
        //印章授权时间
        newSeal.setSealStartTime(sealApply.getSealStartTime());
        newSeal.setSealEndTime(sealApply.getSealEndTime());
        //UK上的授权时间和信息
        newSeal.setAuthorizationTime(sealApply.getAuthorizationTime());
        newSeal.setAuthorizationInfo(sealApply.getAuthorizationInfo());
        //keyId
        newSeal.setKeyId(sealApply.getKeyId());
        //是否UK印章
        newSeal.setIsUK(sealApply.getIsUK());
        //印章状态有效
        newSeal.setSealState(Constant.SEAL_STATE_VALID);
        newSeal.setCardType(sealApply.getCardType());

        //没有异常并且旧印章不为null
        if("success".equals(messageBeen.getMessage()) && sealService.findSealById(sealApply.getSealId())!=null){
            try {

                //删除原来的印章
                Seal seal = new Seal();
                seal.setSealId(sealApply.getSealId());
                sealService.delSeal(seal);
                //添加印章ID相同的新印章
                sealService.addSeal(newSeal);
                certificateService.updateCertificate(certificate);
                sealService.updateSealApply(sealApply);
                messageBeen.setBody(newSeal);

                //发送消息通知申请人确认重做印章
                //向消息中心发消息
                Message message_new_1  = new Message();
                message_new_1.setMessageNo(getEssUUID(newSeal.getUnitId()));
                message_new_1.setSender(((User) session.getAttribute("user")).getUserId());
                message_new_1.setReceiver(sealApply.getApplyUserId());
                message_new_1.setSendTime(getDate());
                message_new_1.setMessageType("3");
                message_new_1.setMessageTitle("重做成功");
                message_new_1.setMessageContent("您申请"+sealApply.getSealName()+"重做的印章已审核制作完成！");
                message_new_1.setApplyInfoId(sealApply.getSealApplyId());
                message_new_1.setReadState(2);
                message_new_1.setState(1);
                messageService.addMessage(message_new_1);

                logService.addSystemLog("制作了"+newSeal.getSealName(),"印章制作",
                        newSeal.getUnitId(),(getLoginUser(session).getUserId()),"");
            }catch (Exception e){
                messageBeen.setMessage("error");
            }
        }
        return FastJsonUtil.toJSONString(messageBeen);
    }

    /**
     *延期
     * @param sealApplyId
     */
    @RequestMapping(value="/make_delay_do.html", method = RequestMethod.POST)
    @ResponseBody
    public String make_delay_do(String sealApplyId) throws IOException {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        SealApply sealApply = sealService.findSealApplyById(sealApplyId);
        if(sealApply!=null){
            //状态修改为制作完成
            sealApply.setApplyState(Constant.MAKE_COMPLETION);
            //设置制作人和制作时间
            sealApply.setMakeUserId(((User) session.getAttribute("user")).getUserId());
            sealApply.setMakeTime(getDate());


        }else{
            messageBeen.setMessage("error");
        }

        if("success".equals(messageBeen.getMessage())){
            try {
                //更新申请信息数据
                sealService.updateSealApply(sealApply);
                //向消息中心发消息
                Message message_new_2  = new Message();
                message_new_2.setMessageNo(getEssUUID(sealApply.getUnitId()));
                message_new_2.setSender(((User) session.getAttribute("user")).getUserId());
                message_new_2.setReceiver(sealApply.getApplyUserId());
                message_new_2.setSendTime(getDate());
                message_new_2.setMessageType(Constant.Message_Type_auDelay);
                message_new_2.setMessageTitle("授权延期确认");
                message_new_2.setMessageContent("您申请"+sealApply.getSealName()+"的授权延期已审核制作完成,请您确认生效！");
                message_new_2.setApplyInfoId(sealApply.getSealApplyId());
                message_new_2.setReadState(2);
                message_new_2.setState(1);
                messageService.addMessage(message_new_2);

                User user =  getLoginUser(session);
                logService.addSystemLog("制作了"+sealApply.getSealName(),"印章制作",
                        sealApply.getUnitId(),user.getUserId(),"");

            }catch (Exception e){
                messageBeen.setMessage("error");
            }
        }
        return new String(FastJsonUtil.toJSONString(messageBeen).getBytes("utf8"),"iso8859-1");
    }

    /**
     *制作驳回
     * @param sealApplyId 申请信息ID
     */
    @RequestMapping(value="/make_reject.html", method = RequestMethod.GET)
    @ResponseBody
    public String seal_make_reject(Model model, String sealApplyId, String message) throws UnsupportedEncodingException {

        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        SealApply sealApply = sealService.findSealApplyById(sealApplyId);

        if(sealApply!=null){
            //向消息中心添加数据
            Message message_new  = new Message();
            message_new.setMessageNo(Constant.JIANGSU_CODE+getUUID());
            message_new.setSender(((User) session.getAttribute("user")).getUserId());
            message_new.setReceiver(sealApply.getReviewUserId());
            message_new.setSendTime(getDate());
            message_new.setMessageType("驳回信息");
            message_new.setMessageTitle("申请"+sealApply.getSealName()+"被驳回");
            message_new.setMessageContent(message);
            message_new.setApplyInfoId(sealApply.getSealApplyId());
            message_new.setReadState(2);
            message_new.setState(1);
            boolean result = messageService.addMessage(message_new);

            //信息状态修改为审核人驳回
            sealApply.setApplyState(Constant.MAKE_NO_THROUGH);
            sealService.updateSealApply(sealApply);
            //添加日志
            User user =  (User) session.getAttribute("user");
            logService.addSystemLog("驳回了"+sealApply.getSealName()+"的制作申请","印章申请驳回",
                    sealApply.getUnitId(),user.getUserId(),"");
        }else{
            messageBeen.setMessage("error");
        }


        return new String(FastJsonUtil.toJSONString(messageBeen).getBytes("utf8"),"iso8859-1");
    }



    /**
     *更新印章keyID
     * @param seal 印章
     */
    @RequestMapping(value="/updateSealForKeyId.html", method = RequestMethod.POST)
    @ResponseBody
    public String updateSealForKeyId(Seal seal) {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        boolean a = sealService.updateSeal(seal);
        if(!a){
            messageBeen.setMessage("error");
        }
        return FastJsonUtil.toJSONString(messageBeen);
    }

    /**
     *更新印章ID
     * @param sealApplyId 申请信息ID
     */
    @RequestMapping(value="/sealApplyFail.html", method = RequestMethod.POST)
    @ResponseBody
    public String sealApplyFail(String sealApplyId) {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        SealApply sealApply = sealService.findSealApplyById(sealApplyId);
        //删除印章
        Seal seal = new Seal();
        seal.setSealId(sealApply.getSealId());
        sealService.delSeal(seal);

        sealApply.setApplyState(Constant.MAKE_NO_COMPLETION);

        sealService.updateSealApply(sealApply);

        Certificate certificate = new Certificate();
        certificate.setCertificateId(sealApply.getCertificateId());
        certificate.setFileState(Constant.FILE_STATE_NULL);
        certificateService.updateCertificate(certificate);

        return FastJsonUtil.toJSONString(messageBeen);
    }
    /**
     *访问证书延期页面
     * @param certificateId
     */
    @RequestMapping(value="/cer_delay.html", method = RequestMethod.GET)
    public String cer_delay(String certificateId) {

        //根据证书id查找到所属印章的信息
        Seal seal= new Seal();
        seal.setCerId(certificateId);
        List<Seal> sealList = sealService.findSealList(seal);

        if(sealList.size()>=1){
            seal = sealList.get(0);
        }

        //新建申请信息
        //根据印章延期申请制作新的申请信息
        SealApply sealApply = new SealApply();
        //设置新申请id
        sealApply.setSealApplyId(getEssUUID(seal.getUnitId()));
        //设置申请的印章Id
        sealApply.setSealId(seal.getSealId());
        //设置申请的类别 证书延期
        sealApply.setApplyType(Constant.APPLYTYPE_DELAY_CER);

        //设置延期起始时间 ()
        sealApply.setSealStartTime(seal.getSealStartTime());
        //设置延期到期时间 ()
        sealApply.setSealEndTime(seal.getSealEndTime());

        sealApply.setUnitId(seal.getUnitId());
        sealApply.setSealTypeId(seal.getSealTypeId());
        sealApply.setSealImgId(seal.getSealImgId());
        sealApply.setSealName(seal.getSealName());


        Certificate new_c = seal.getCertificate();
        new_c.setCertificateId(getEssUUID(seal.getUnitId()));
        new_c.setFileState(Constant.FILE_STATE_NULL);
        new_c.setCerBase64("");
        new_c.setPfxBase64("");

        new_c.setApplyTime(getDate());

        String cId = certificateService.addCertificate(new_c);

        sealApply.setCertificateId(cId);


        sealApply.setIsUK(seal.getIsUK());
        sealApply.setKeyId(seal.getKeyId());
        sealApply.setSealHwUserIdNum(seal.getSealHwIdNum());
        sealApply.setFileTypeNum(seal.getFileTypeNum());
        sealApply.setAuthorizationInfo(seal.getAuthorizationInfo());
        sealApply.setAuthorizationTime(seal.getAuthorizationTime());


        //设置申请人信息
        //设置申请人和申请时间
        sealApply.setApplyUserId(((User) session.getAttribute("user")).getUserId());
        sealApply.setApplyTime(getDate());
        //设置审核人信息
        //设置审核时间
        sealApply.setReviewUserId(((User) session.getAttribute("user")).getUserId());
        sealApply.setReviewTime(getDate());


        sealApply.setApplyState(Constant.CER_DELAY_APPLY);
        //向数据库添加申请信息
        sealService.addSealApply(sealApply);

        User user =  (User) session.getAttribute("user");
        logService.addSystemLog("申请延期证书"+sealApply.getSealName(),"证书延期",
                sealApply.getUnitId(),user.getUserId(),"");

        return "redirect:/make/make_detail.html?sealApplyId="+sealApply.getSealApplyId();
    }


    /**
     *延期
     * @param sealApplyId 申请信息ID
     */
    @RequestMapping(value="/cer_delay_do.html", method = RequestMethod.POST)
    @ResponseBody
    public String cer_delay_do(String cardType,String sealApplyId,String startTime,String endTime,String cerIssuer) throws IOException {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);
        SealApply sealApply = null;
        Certificate c =null;
        Seal seal =new Seal();;


        //查出完整信息
        sealApply = sealService.findSealApplyById(sealApplyId);
        sealApply.setCardType(cardType);
        //状态修改为制作完成
        sealApply.setApplyState(Constant.MAKE_COMPLETION);
        //设置制作人和制作时间
        sealApply.setMakeUserId(((User) session.getAttribute("user")).getUserId());
        sealApply.setMakeTime(getDate());
        //新的授权单位
        sealApply.setCerIssuer(cerIssuer);

        c = sealApply.getCertificate();
        c.setStartTime(startTime);
        c.setEndTime(endTime);
        //原有颁发单位
        c.setIssuerUnitId(cerIssuer);

        seal.setUnitId(sealApply.getUnitId());

        if(sealApply.getIsUK()==1){
            //是UK
            c.setFileState(Constant.FILE_STATE_CER);
        }else{
            //不是UK
            c.setFileState(Constant.FILE_STATE_CERANDPFX);

            Map<String, String> cerAndPfxMap =  certificateService.createCerFileAndPfx(c);

            c.setPfxBase64(cerAndPfxMap.get("pfxBase64"));
            c.setCerBase64(cerAndPfxMap.get("cerBase64"));
            //此处将seal的证书Id修改
            seal.setSealId(sealApply.getSealId());
            seal.setCerId(c.getCertificateId());

        }
//        try{
//
//        }catch (Exception e){
//            messageBeen.setMessage("error");
//        }

        if("success".equals(messageBeen.getMessage())){
            certificateService.updateCertificate(c);
            sealService.updateSeal(seal);
            sealService.updateSealApply(sealApply);
            messageBeen.setBody(sealService.findSealById(sealApply.getSealId()));
            User user =  getLoginUser(session);
            logService.addSystemLog("延期了"+sealApply.getSealName(),"印章的证书",
                    sealApply.getUnitId(),user.getUserId(),"");
        }
        return new String(FastJsonUtil.toJSONString(messageBeen).getBytes("utf8"),"iso8859-1");
    }


    /**
     *更新印章证书ID
     * @param sealApplyId 申请信息ID
     */
    @RequestMapping(value="/changeCerId.html", method = RequestMethod.POST)
    @ResponseBody
    public String changeCerId(String sealApplyId,String certificateId) {
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        SealApply sealApply = sealService.findSealApplyById(sealApplyId);
        //删除印章
        Seal seal = new Seal();
        seal.setSealId(sealApply.getSealId());
        seal.setCerId(certificateId);

        sealService.updateSeal(seal);

        return FastJsonUtil.toJSONString(messageBeen);
    }
    /**
     *导出待制作印章需要申请证书的信息
//     * @param unitId 申请信息
     */
    @RequestMapping(value="/exportCertInfo.html", method = RequestMethod.GET)
    @ResponseBody
    public void exportCertInfo(String unitId) {

        SealApply sealApply = new SealApply();
        sealApply.setUnitId(unitId);
        sealApply.setIsUK(2);

        //审核通过
        sealApply.setApplyState(Constant.REVIEW_THROUGH);
        List<SealApply> sealApplyList =  sealService.findSealApply(sealApply);
        //制作失败
        sealApply.setApplyState(Constant.MAKE_NO_COMPLETION);
        List<SealApply> sealApplyList_1 =  sealService.findSealApply(sealApply);
        sealApplyList.addAll(sealApplyList_1);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("统一单位编码").append("       ");
        stringBuffer.append("印章编码").append("           ");
        stringBuffer.append("有效期起始").append("   ");
        stringBuffer.append("有效期到期").append("   ");
        stringBuffer.append("印章名称").append("          ");
        stringBuffer.append("单位名称").append("          \r\n");
        for(SealApply sa : sealApplyList){
            String unitName =  sa.getUnit().getUnitName();
            String businessUnitId = unitService.findBusinessUnitId(sa.getUnitId());
            String sealName = sa.getSealName();
            String sealCode = sa.getCertificateId();
            String s = sa.getSealStartTime();
            String e = sa.getSealEndTime();
            stringBuffer.append(businessUnitId).append("   ");
            stringBuffer.append(sealCode).append("   ");
            stringBuffer.append(s).append("   ");
            stringBuffer.append(e).append("   ");
            stringBuffer.append(sealName).append("   ");
            stringBuffer.append(unitName).append("   \r\n");
        }


        String fileName;
        if(sealApplyList.get(0)!=null){
            String unitName = sealApplyList.get(0).getUnit().getUnitName();
            fileName = unitName +"待申请证书信息.txt";
        }else{
            fileName = "待申请证书信息.txt";
        }
        OutputStream os = null;
        try {
            response.reset();
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(),
                    "ISO8859-1"));
            byte[] bytes = stringBuffer.toString().getBytes("GBK");
            os = response.getOutputStream();
            // 将字节流传入到响应流里,响应到浏览器
            os.write(bytes);
            os.close();
        } catch (Exception ex) {
//            logger.error("导出失败:", ex);
            throw new RuntimeException("导出失败");
        }finally {
            try {
                if (null != os) {
                    os.close();
                }
            } catch (IOException ioEx) {
//                logger.error("导出失败:", ioEx);
            }
        }
    }

    /**
     *解析pfx证书
     * @param pfxBase64 pfx证书base64编码
     * @param cerPsw 证书密码
     */
    @RequestMapping(value="/verifyPfx.html", method = RequestMethod.POST,produces="text/html;charset=UTF-8")
    @ResponseBody
    public String verifyPfx(String pfxBase64, String cerPsw){
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);
        if(isNull(pfxBase64)&&isNull(cerPsw)){
            //当证书和密码都不为空时
            Map<String,String> cerInfo = new HashMap<>();
            try {
                //解析pfx证书的信息
                cerInfo = ReadPfx.GetCertInfoFromPfxBase64(pfxBase64,cerPsw);
            } catch (Exception e) {
                errorLogService.addErrorLog("ApplyController-getPfxInfo-解析pfx证书出错-null！");
                messageBeen.setMessage("error");
                e.printStackTrace();
            }
            //证书所有人信息
            String owner = cerInfo.get("owner");

            String[] a = owner.split(", ");
            Map<String,String> ownerInfo = new HashMap<>();
            ownerInfo.put(a[a.length-1].split("=")[0],a[a.length-1].split("=")[1]);
            ownerInfo.put(a[a.length-2].split("=")[0],a[a.length-2].split("=")[1]);
            ownerInfo.put(a[a.length-3].split("=")[0],a[a.length-3].split("=")[1]);
            ownerInfo.put(a[a.length-4].split("=")[0],a[a.length-4].split("=")[1]);
            ownerInfo.put(a[a.length-5].split("=")[0],a[a.length-5].split("=")[1]);
            ownerInfo.put(a[a.length-6].split("=")[0],a[a.length-6].split("=")[1]);

            messageBeen.setBody(ownerInfo);
        }else{
            messageBeen.setMessage("error");
        }
        return FastJsonUtil.toJSONString(messageBeen);
    }

    @RequestMapping(value="/importSeal.html", method = RequestMethod.POST,produces="text/html;charset=UTF-8")
    @ResponseBody
    public String importSeal(String unitId,String imgBase64,String sealName,String idNum,String city,String unitName,
                             String departName,String certName) throws InterruptedException {
        //需要的参数，单位id  印章id 名称 图像base64  录入人id 025user1  录入时间 2019-03-19 10:46:06
        //印章类型 025st7 身份证号 文档类型编码 127  印章开始时间 2019-04-01 结束时间 2024-04-01  isUK 2
        //sealState 1 证书
        //根据已有信息生成印章对象
        //生成印章图像
        SealImg sealImg = new SealImg();
        sealImg.setSealImgId(getEssUUID(unitId));
        sealImg.setSealImgGifBase64(imgBase64);
        sealImg.setSealImageType("gif");
        sealImg.setSealThumbnailImgBase64(imgBase64);
        //添加图片
        String sealImgId = sealImgService.addSealImg(sealImg);


        //证书相关的处理
        Certificate certificate =new Certificate();
        String businessUnitId = unitService.findBusinessUnitId(unitId);
//        sleep(1001);
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        //后缀
        String suffix = formatter.format(currentTime);
        //证书id = 印章编码 = 统一单位编码+印章类型id+同单位下同类型印章个数
        String certId = businessUnitId+"025st7"+suffix;

        certificate.setCertificateId(getEssUUID(unitId));
        certificate.setCountry("中国");
        certificate.setProvince("江苏省");
        certificate.setCity(city);
        certificate.setCertUnit(unitName);
        certificate.setCertDepartment(departName);
        certificate.setCerName(certName);

        //设置证书的颁发者单位
        certificate.setIssuerUnitId("004");
        //手签 自己生成证书 证书起始时间 根据印章起始时间决定
        certificate.setStartTime("2019-04-01");
        certificate.setEndTime("2024-04-01");
        certificate.setAlgorithm(Constant.CER_ALGORITHM);
        //设置证书信息：种类
        certificate.setCerClass(Constant.CER_CLASS);
        //设置证书信息：版本
        certificate.setCertificateVersion(Constant.CER_VERSION);
        //设置证书信息有效
        certificate.setState(Constant.STATE_YES);
        //密码
        certificate.setCerPsw(StringUtils.getESSPwd());
        certificate.setFileState(Constant.FILE_STATE_CERANDPFX);
        //生成证书
        Map<String, String> cerAndPfxMap =  certificateService.createCerFileAndPfx(certificate);

        certificate.setPfxBase64(cerAndPfxMap.get("pfxBase64"));
        certificate.setCerBase64(cerAndPfxMap.get("cerBase64"));
        //生成证书hash
        byte[] essGetDigest ;
        try {
            essGetDigest = MuticaCrypt.ESSGetDigest(ESSGetBase64Decode(cerAndPfxMap.get("cerBase64")));
            String certHash = MuticaCrypt.ESSGetBase64Encode(essGetDigest);
            certificate.setCertHash(certHash);
        } catch (MuticaCryptException e) {
            errorLogService.addErrorLog("ApplyController-ESSGetDigest-证书生成错误-null！");
        }
        //添加证书
        certificateService.addCertificate(certificate);

        Seal newSeal = new Seal();
        //印章Id
        newSeal.setSealId(getEssUUID(unitId));
        //印章名称
        newSeal.setSealName(sealName);
        //印章图像
        newSeal.setSealImgId(sealImgId);
        //印章证书
        newSeal.setCerId(certificate.getCertificateId());
        //制作时间
        newSeal.setInputTime("2019-03-21 00:00:00");
        //制作人
        newSeal.setInputUserId("025user1");
        //印章单位
        newSeal.setUnitId(unitId);
        //印章类型
        newSeal.setSealTypeId("025st7");
        //手签身份证号
        newSeal.setSealHwIdNum(idNum);
        //印章授权
        newSeal.setFileTypeNum(127);
        //印章授权时间
        newSeal.setSealStartTime("2019-04-01");
        newSeal.setSealEndTime("2024-04-01");
        //是否UK印章
        newSeal.setIsUK(2);
        //印章状态有效
        newSeal.setSealState(Constant.SEAL_STATE_VALID);
//        newSeal.setCardType(sealApply.getCardType());

        //在这里更新个人的证书
        Person p =new Person();
        p.setIdNum(idNum);
        p.setCerId(certificate.getCertificateId());
        personDao.updatePersonByIDNum(p);

        try {
            sealService.addSeal(newSeal);
            logService.addSystemLog("制作了"+newSeal.getSealName(),"印章制作",
                    newSeal.getUnitId(),"025user1","");
        }catch (Exception e){
            e.printStackTrace();
        }
        return "success";
    }

    /**

     */
    @RequestMapping(value="/register_uk.html", method = RequestMethod.POST,produces="text/html;charset=UTF-8")
    @ResponseBody
    public String register_uk(String pfxBase64, String cerPsw){
        ResultMessageBeen messageBeen = new ResultMessageBeen();
        messageBeen.setMessage("success");
        messageBeen.setBody(null);

        //录入印章信息



        return FastJsonUtil.toJSONString(messageBeen);
    }


}
