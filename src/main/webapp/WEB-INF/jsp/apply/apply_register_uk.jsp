<%--
  Created by IntelliJ IDEA.
  User: 陈晓坤
  Date: 2018/9/13
  Time: 14:04
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-cn">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <meta name="renderer" content="webkit">
    <title></title>
    <script src="${pageContext.request.contextPath}/js/jquery-3.2.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/pintuer.js"></script>
    <script src="${pageContext.request.contextPath}/js/my.js"></script>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/pintuer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/mask.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/jquery-dialogbox/jquery.dialogbox-1.0.css">
    <script src="${pageContext.request.contextPath}/jquery-dialogbox/jquery.dialogbox-1.0.js"></script>
    <!--三级联动-->
    <script src="${pageContext.request.contextPath}/distpicker/js/distpicker.data.js"></script>
    <script src="${pageContext.request.contextPath}/distpicker/js/distpicker.js"></script>
    <script src="${pageContext.request.contextPath}/distpicker/js/main.js"></script>

    <style type="text/css">
        .w300{
            width: 300px;
            float:left;
        }
        .w100{
            width: 100px;
            float:left;
        }
        .w150{
            width: 150px;
            float:left;
        }
        .w200{
            width: 200px;
            float:left;
        }
        .form-x .form-group .label {
            width: 170px;
        }
        .form-x .form-group .field {
            width: calc(100% - 170px);
        }
    </style>
</head>
<body>
<div class="panel admin-panel">
    <div class="panel-head"><strong class="icon-reorder"> ESS签章制作系统--申请新印章</strong> <a href="" style="float:right; display:none;">添加字段</a></div>
    <div class="padding border-bottom form-x" >
        <ul class="search" style="padding-left:10px;">
            <li><a class="button border-main icon-plus-square-o" href="javaScript:window.location.href = document.referrer;"> 返回</a></li>
        </ul>
    </div>
    <form method="post" class="form-x" id="applyInfo_from" action="${pageContext.request.contextPath}/apply/add_do.html" enctype="multipart/form-data">
        <div class="body-content">
            <ul class="search">
                <li class="label">
                    <strong class="icon-reorder">印章信息</strong>
                </li>
                <li>申请UK印章
                    <select id="isUK" name="isUK"  onchange="ukChange()" style="width:70px; line-height:17px; display:inline-block">
                        <option value="2">否</option>
                        //暂时隐藏uk申请接口
                        <%--<option value="1">是</option>--%>
                    </select>
                    &nbsp;&nbsp;


                    &nbsp;&nbsp;
                </li>
                <%--<li id="searchPerson" style="display: none">--%>
                <%--<input type="text" placeholder="请输入搜索关键字" id="keywords"  style="width:150px; line-height:17px;display:inline-block" />--%>
                <%--<a href="javascript:void(0)" class="icon-search" onclick="searchPerson()"> 搜索</a>--%>
                <%--</li>--%>
            </ul>
            <ul class="search" >
                <li class="form-group">
                    <div class="label">
                        <label> 印章类型：</label>
                    </div>
                    <%--style="width:150px; line-height:17px;display:inline-block"--%>
                    <div class="field">
                        <select id="sealTypeId" name="sealTypeId" onchange="changeSealType()" class="input w300">
                            <c:forEach items="${sealTypes}" var="item"  varStatus="status">
                                <option value="${item.sealTypeId}">${item.sealTypeName}</option>
                            </c:forEach>
                        </select>
                    </div>

                </li>
                <li class="form-group" id="searchPerson" style="width:360px;">
                    <div class="field" >
                        <input type="text" placeholder="输入人员关键字" id="keywords" class="input" style="width: 200px;"/>
                    </div>
                    <button type="button" class="button icon-search" style="width: 100px;" onclick="searchPerson()">查找</button>
                </li>
            </ul>
            <ul class="search" >
                <%--申请信息类别--%>
                <input type="hidden" id="applyType" name="applyType" value="${applyType}"/>
                <%--单位id--%>
                <input type="hidden" id="unitId" name="unitId" value="${unit.unitId}"/>
                <%--单位id--%>
                <input type="hidden" id="unitName" name="unitName" value="${unit.unitName}"/>
                <li class="form-group">
                    <div class="label">
                        <label>印章名称：</label>
                    </div>
                    <div class="field">
                        <input type="text"  class="input w300" id="sealName" name="sealName" value="" />
                    </div>
                </li>
            </ul>
            <ul class="search" id="person_div" style="display: none">
                <li class="form-group" >
                    <div class="label">
                        <label>手签人标识：</label>
                    </div>
                    <div class="field">
                        <input type="text"  class="input w100"  id="personName" value="" readonly />
                        <%--<input type="hidden"  name="sealHwUserIdNum" id="sealHwUserIdNum" value="" readonly />--%>
                    </div>
                </li>
                <li class="form-group" >
                    <div class="field">
                        <%--身份证号--%>
                        <input type="text"  class="input w200" name="sealHwUserIdNum" id="sealHwUserIdNum" value="" readonly />
                    </div>
                </li>
            </ul>
            <ul class="search"  >
                <li class="form-group">
                    <div class="label">
                        <label>授权种类：</label>
                    </div>
                    <div class="field">
                        <c:forEach items="${fileTypeList}" var="item"  varStatus="status">
                            <input type="checkbox" value="${item.fileTypeValue}" name="sProblem">${item.fileTypeName}
                        </c:forEach>
                        <%--授权类型编码--%>
                        <input type="hidden" name="fileTypeNum" id="fileTypeNum">
                    </div>
                </li>
            </ul>
            <ul class="search" >
                <li class="form-group">
                    <div class="label">
                        <label>附件：</label>
                    </div>
                    <div class="field">
                        <div class="uploader white">
                            <input type="text" id="fileName" value="图片或者PDF文件（不超过4M）" class="filename input w300" readonly/>
                            <input type="button" class="button1" value="添加文件"/>
                            <input type="file" id="attachmentFile"  name="attachmentFile" size="30"/>
                        </div>
                    </div>
                </li>
            </ul>
            <ul class="search"  >
                <li class="label">
                    <strong class="icon-reorder">证书信息</strong>
                </li>
                <%--<li class="form-group">--%>
                <%--使用外部证书--%>
                <%--<select id="isPfx" onchange="changePfx()"  style="width:60px; line-height:17px;display:inline-block">--%>
                <%--<option value="1">是</option>--%>
                <%--<option value="2">否</option>--%>
                <%--</select>--%>
                <%--&nbsp;&nbsp;--%>
                <%--</li>--%>
            </ul>
            <ul class="search" >
                <li class="form-group" >
                    <div class="label">
                        <label>所在国家：</label>
                    </div>
                    <div class="field">
                        <input type="text"  class="input w300" id="country" name="country" value="中国" />
                    </div>
                </li>
                <li class="form-group" >
                    <div class="label">
                        <label>所在省份：</label>
                    </div>
                    <div class="field">

                        <input type="text"  class="input w300" id="province" name="province" value="江苏省"/>
                    </div>
                </li>
                <li class="form-group">
                    <div class="label">
                        <label>所在城市：</label>
                    </div>
                    <div class="field">
                        <input type="text"  class="input w300" id="city" name="city" />
                    </div>
                </li>
                <li class="form-group">
                    <div class="label">
                        <label>单位名称：</label>
                    </div>
                    <div class="field">
                        <input type="text" class="input w300" name="certUnit" id="certUnit"  value="${unit.unitName}"/>
                    </div>
                </li>
                <li class="form-group">
                    <div class="label">
                        <label>部门名称：</label>
                    </div>
                    <div class="field">
                        <input type="text"  class="input w300" id="certDepartment" name="certDepartment"  />
                    </div>
                </li>
                <li class="form-group">
                    <div class="label">
                        <label>公用名称：</label>
                    </div>
                    <div class="field">
                        <input type="text"  class="input w300" id="cerName" name="cerName"  />
                    </div>
                </li>
            </ul>

            <ul class="search" >
                <li class="form-group">
                    <div class="label">
                        <label></label>
                    </div>
                    <div class="field">
                        <button type="button" class="button bg-main icon-check-square-o w150" onclick="sealInfoSubmit()">录入印章</button>
                    </div>
                </li>
            </ul>
        </div>
    </form>
</div>
</body>
<script>
    /**
     *初始化工作
     */
    $(function(){

    });
    /**
     *提交申请信息
     */
    function sealInfoSubmit() {
        $("#fileTypeNum").val(getTheCheckBoxValue());
        var formData = new FormData($("#applyInfo_from")[0]);
        var verificResult  = verification();
        // var verificResult = true;
        if(verificResult=="true"){
            $.ajax({
                type: "post",
                url: "${pageContext.request.contextPath}/make/register_uk.html",
                data: formData,
                cache: false,
                processData: false,
                contentType: false,
                success: function (data)
                {
                    var obj = eval('('+ data +')');
                    if(obj.message == "success"){
                        $('body').dialogbox({
                            type:"normal",title:"申请成功",
                            buttons:[{
                                Text:"确认",
                                ClickToClose:true,
                                callback:function (dialog){
                                    window.location.href = document.referrer;
                                }
                            }],
                            message:'已根据您提交的信息成功提交印章申请，请您等候审核制作！'
                        });
                    }else{
                        $('body').dialogbox({
                            type:"normal",title:"申请失败",
                            buttons:[{
                                Text:"确认",
                                ClickToClose:true,
                                callback:function (dialog){
                                    // window.history.go(-1)
                                }
                            }],
                            message:'申请提交失败，请您重试！'
                        });
                    }
                }
            });
        }else{
            $('body').dialogbox({
                type:"normal",title:"信息缺失",
                buttons:[{
                    Text:"确认",
                    ClickToClose:true,
                    callback:function (dialog){
                    }
                }],
                message:verificResult
            });
        }
    }


    function verification(){
        if(isNull($("#sealName").val())){
            return "印章名称不可为空！";
        }
        if(isNull($("#province").val()) ||isNull($("#city").val()) ||isNull($("#certUnit").val())
            ||isNull($("#certDepartment").val()) ||isNull($("#cerName").val())){
            return "请完整填写证书信息！";
        }
        return "true";
    }

    /**
     * 异步查询人员
     */
    function searchPerson() {
        $.ajax({
            url: "${pageContext.request.contextPath}/apply/findPerson.html",
            type: "get",
            data: {"keyword":$("#keywords").val()},
            success: function (data) {
                var obj = eval('('+ data +')');
                if(obj.message=="ESSSUCCESS"){
                    //成功
                    var arr=new Array();
                    arr.push("<table class=\"table table-hover text-center\">");
                    arr.push("<tr><th width=\"50px\">选择</th><th width=\"50px\">图像</th><th width=\"90px\">手机号</th><th width=\"60px\">名字</th><th width=\"140px\">身份证号</th></tr>");
                    $.each(obj.body,function(name,value) {
                        arr.push("<tr>");
                        arr.push("<td><input type=\"radio\" name=\"companyRdoID\" id=\"companyRdoID1\" value=\""+value.personId+"\"></td>");
                        arr.push("<td><img src=\"data:image/gif;base64,"+value.personImgBase64+"\" width=\"30px\" height=\"30px\"/></td>");
                        arr.push("<td id=\""+value.personId+"phone\">"+value.phone+"</td>");
                        arr.push("<td id=\""+value.personId+"name\">"+value.personName+"</td>");
                        arr.push("<td id=\""+value.personId+"idNum\">"+value.idNum+"</td>");
                        arr.push("</tr>");
                    });
                    arr.push("</table>");
                    $('body').dialogbox({
                        type:"normal",title:"人员搜索",
                        buttons:[{
                            Text:"确认",
                            ClickToClose:true,
                            callback:function (dialog){
                                var personId = $(dialog).find("input[name='companyRdoID']:checked").val();
                                $("#personName").val($(dialog).find("#"+personId+"name").text());
                                $("#sealName").val($(dialog).find("#"+personId+"name").text());
                                $("#sealHwUserIdNum").val($(dialog).find("#"+personId+"idNum").text());
                            }
                        }],
                        message:arr
                    });
                } else if(obj.message=="ESSERROR"){
                    $('body').dialogbox({
                        type:"normal",title:"人员搜索",
                        buttons:[{
                            Text:"确认",
                            ClickToClose:true,
                            callback:function (dialog){
                            }
                        }],
                        message:obj.body
                    });
                }

            },
            error: function (data) {
                $('body').dialogbox({
                    type:"normal",title:"服务器错误！",
                    buttons:[{
                        Text:"确认",
                        ClickToClose:true,
                        callback:function (dialog){
                        }
                    }],
                    message:"请求的服务器或数据错误！请重试！"
                });
            }
        });
    }

</script>
</html>