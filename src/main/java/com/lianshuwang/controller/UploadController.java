package com.lianshuwang.controller;

import com.lianshuwang.domin.Book;
import com.lianshuwang.helper.BookHelper;
import com.lianshuwang.helper.UserHelper;
import com.lianshuwang.service.BookService;
import com.lianshuwang.service.UserService;
import com.lianshuwang.util.PropertyConfigurer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


@Controller
public class UploadController {

    @Autowired
    private BookService bookService;
    @Autowired
    private UserService userService;

    private static final Log logger = LogFactory.getLog(UploadController.class);


    //是否登录
    @RequestMapping(value = "getUploadPage")
    public String getUploadPage(HttpSession session) {
        UserHelper userHelper = (UserHelper) session.getAttribute("userHelper");
        if (null == userHelper) {
            return "redirect:index";
        }
        logger.info("you are visiting uploading page!");
        return "upload";
    }

    //文件上传
    @RequestMapping(value = "doUpload", method = RequestMethod.POST)
    public String doUpload(@ModelAttribute BookHelper bookHelper, Model model, HttpSession session ) throws IllegalStateException, IOException, ParseException {
        logger.info("you are uploading a book! ");
        logger.info("This book is " + bookHelper.getTitle() + "!");

        String fileName = bookHelper.getBookFile().getOriginalFilename();
        String bookCover = bookHelper.getBookCover().getOriginalFilename();
        MultipartFile bookFile = bookHelper.getBookFile();
        MultipartFile coverFile = bookHelper.getBookCover();

        if (bookFile.isEmpty()) {
            logger.info("Uploading failed! The book you are uploading is empty!");
            return "upload_failed";
        } else if (coverFile.isEmpty()) {
            logger.info("Uploading failed! The book cover you are uploading is empty!");
            return "upload_failed";
        } else {
            //大类编号拼上小类编号
            String typeId = "" + bookHelper.getLargeType() + bookHelper.getSmallType();
            int type_id = Integer.parseInt(typeId);
            //截取字符串（后缀，txt pdf 等）
            String format = fileName.substring(fileName.lastIndexOf('.') + 1);
            List<String> typeNames;
            typeNames = bookService.getTypeNames(type_id);
            String filePath_pre = (String) PropertyConfigurer.getProperty("book_path");
            String filePath = filePath_pre + typeNames.get(0) +
                    "/" + typeNames.get(1) + "/" +
                    bookHelper.getTitle() + "." + format;

            //判断上传图书是否重复
            File localBookFile = new File(filePath);
            if (localBookFile.exists()) {
                logger.info("Uploading failed! The book is existed!");
                return "upload_failed2";
            }
            bookFile.transferTo(localBookFile);
            //封面图片
            String coverPath_pre = (String) PropertyConfigurer.getProperty("book_cover_path");
            String coverPath = coverPath_pre + typeNames.get(0) +
                    "/" + typeNames.get(1) + "/" +
                    bookHelper.getTitle() + ".jpg";
            File localCoverFile = new File(coverPath);
            coverFile.transferTo(localCoverFile);
            logger.info("The book has uploaded to local path successfully!");


            Book book = new Book();
            book.setBook_title(bookHelper.getTitle());
            book.setBook_author(bookHelper.getAuthor());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
            Date date = dateFormat.parse(bookHelper.getPubYear());
            //出版日期
            book.setBook_pubYear(date);
            //图书简介
            book.setBook_summary(bookHelper.getSummary());
            book.setType_id(type_id);
            //文件格式
            book.setBook_format(format);
            //下载次数
            book.setDownload_times(0);
            book.setBook_file(filePath);
            book.setBook_cover(coverPath);
            //日期格式
            dateFormat = new SimpleDateFormat("yyMMdd", Locale.CHINESE);
            //设置当前国家或地区的时间，具体到日
            String pubDate = dateFormat.format(date);
            String upDate = dateFormat.format(new Date());
            //伪随机数.包括0不包括900
            int random = new Random().nextInt(900) + 100;
            //bookId生成
            String idStr = "" + typeId + pubDate + upDate + random;
            long bookID = Long.parseLong(idStr);
            logger.info("The book id you uploaded is " + bookID);

            book.setId(bookID);
            bookService.uploadBook(book);
            UserHelper userHelper = (UserHelper) session.getAttribute("userHelper") ;
            //更新upload表，主要记录上传时间
            bookService.updateRecords(userHelper.getId(),bookID);
            //增加2分贡献值
            userService.updateUserContribution(2,userHelper.getId());
            model.addAttribute("bookID",bookID);
            logger.info("you are coming to the uploading successful page!");
            return "upload_success";
        }
    }
}