package com.lxing.controller;


import com.lxing.common.util.ExcelUtil;
import com.lxing.domain.Book;
import com.lxing.service.BookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/***
 * Created on 2017/11/3 <br>
 * Description: [BookController]<br>
 * @author lxing
 * @version 1.0
 */
@RestController
@RequestMapping("/books")
@Api(value = "/books", description = "书籍API")
public class BookController {

  @Autowired
  BookService bookService;

  @PostMapping
  @ApiOperation(value = "新增书籍")
  @ApiImplicitParam(dataType = "Book", name = "book", value = "书籍信息", required = true)
  @ApiResponses({
      @ApiResponse(code = 201, message = "新增成功"),
      @ApiResponse(code = 500, message = "接口异常"),
  })
  public ResponseEntity<Book> create(@RequestBody Book book) {
    int isSuccess = bookService.insert(book);
    if (isSuccess == 0) {
      return new ResponseEntity<>(book, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(book, HttpStatus.CREATED);
  }

  @DeleteMapping("/{id:\\d+}")
  public ResponseEntity<Integer> delete(@PathVariable("id") int id) {
    int isSuccess = bookService.deleteById(id);
    if (isSuccess == 0) {
      return new ResponseEntity<>(id, HttpStatus.NOT_FOUND);
    }
    //请求收到但返回结果为空
    return new ResponseEntity<>(id, HttpStatus.OK);
  }


  @PutMapping("/{id:\\d+}")
  public ResponseEntity<Book> update(@RequestBody Book book) {
    int isSuccess = bookService.updateById(book);
    if (isSuccess == 0) {
      return new ResponseEntity<>(book, HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(book, HttpStatus.OK);
  }

  @GetMapping("/{id:\\d+}")
  public ResponseEntity<Book> getInfo(@PathVariable("id") int id) {
    Book book = bookService.selectById(id);
    if (book == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(book, HttpStatus.OK);
  }

  @GetMapping(value = "/excel", produces = {"application/vnd.ms-excel;charset=UTF-8"})
  public ResponseEntity<Book> getExcel(HttpServletResponse response) {
    try {

      String fileName = "纠错统计表";
      List<Map<String, Object>> list = createExcelRecord();
      String columnNames[] = {"发起纠错时间", "纠错人", "负责人", "标准库名", "纠错内容", "纠错状态"};//列名
      String keys[] = {"time", "notifyUser", "beNotifiedUser", "standardName", "content",
          "finish"};//map中的key
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try {
        ExcelUtil.createWorkBook(list, keys, columnNames).write(os);
      } catch (IOException e) {
        e.printStackTrace();
      }
      byte[] content = os.toByteArray();
      InputStream is = new ByteArrayInputStream(content);
      // 设置response参数，可以打开下载页面
      response.reset();
      response.setContentType("application/vnd.ms-excel;charset=utf-8");
      response.setHeader("Content-Disposition",
          "attachment;filename=" + new String((fileName + ".xls").getBytes(), "iso-8859-1")+
              ";filename*=UTF-8''"+ URLEncoder.encode(fileName,"UTF-8")+".xls"
      );
      ServletOutputStream out = response.getOutputStream();
      BufferedInputStream bis = null;
      BufferedOutputStream bos = null;
      try {
        bis = new BufferedInputStream(is);
        bos = new BufferedOutputStream(out);
        byte[] buff = new byte[2048];
        int bytesRead;
        // Simple read/write loop.
        while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
          bos.write(buff, 0, bytesRead);
        }
      } catch (final IOException e) {
        throw e;
      } finally {
        if (bis != null) {
          bis.close();
        }
        if (bos != null) {
          bos.close();
        }
      }
    } catch (Exception e) {

    }

    return null;
  }

  private List<Map<String, Object>> createExcelRecord() {
    List<Map<String, Object>> listmap = new ArrayList<Map<String, Object>>();
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("sheetName", "sheet1");
    listmap.add(map);
    Map<String, Object> mapValue = new HashMap<String, Object>();
    mapValue.put("content", "很不错");
    String finish = "";
    if (finish.equals("0")) {
      finish = "已纠错";
    } else {
      finish = "未纠错";
    }
    mapValue.put("finish", finish);
    listmap.add(mapValue);
    return listmap;
  }

}
