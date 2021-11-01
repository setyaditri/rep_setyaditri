__file dummy__

package com.pratesis.scylla.mobile.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.pratesis.scylla.mobile.models.Aktifitas;
import com.pratesis.scylla.mobile.services.AktifitasService;
import com.pratesis.scylla.mobile.models.ImportAktifitas;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

@Controller
public class AktifitasController {
    private AktifitasService aktifitasService;

    @Autowired
    public void setAktifitasService(AktifitasService aktifitasService) {
        this.aktifitasService = aktifitasService;
    }
    
    @GetMapping("aktifitas")
    String index(Model model) {
        model.addAttribute("aktifitass", aktifitasService.listAllAktifitass());
        return "aktifitas/index";
    }
    
    @RequestMapping(value = "aktifitas/CheckCode", method = RequestMethod.GET)
    String CheckCode(Model model) {
        model.addAttribute("aktifitas", new Aktifitas());
        return "aktifitas/add-edit";
    }
    
    @RequestMapping(value = "aktifitas/create", method = RequestMethod.GET)
    String create(Model model) {
    	model.addAttribute("aktifitas", new Aktifitas());
        return "aktifitas/add-edit";
    }
    
    @RequestMapping("aktifitas/{id}")
    public String show(@PathVariable Long id, Model model) {
    	Aktifitas aktifitas = aktifitasService.getAktifitasById(id);
    	if (aktifitas == null) {
    		return "public/error/404";
    	}
        model.addAttribute("aktifitas", aktifitas);
        return "aktifitas/show";
    }
    
    @RequestMapping(value = "aktifitas/{id}/edit", method = RequestMethod.GET)
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("aktifitas", aktifitasService.getAktifitasById(id));
        return "aktifitas/add-edit";
    } 	

    @RequestMapping(value = "aktifitas", method = RequestMethod.POST)
    public String store(Aktifitas aktifitas, Model model) {
        Aktifitas presentAktivitas = aktifitasService.findOneByCode(aktifitas.getCode());
        String error = null;
        if (presentAktivitas != null){
            error = "Kode harus unik";
            model.addAttribute("status", "failed");
            model.addAttribute("message", error);
            return "aktifitas/add-edit";
        }
        else{
        	aktifitasService.saveAktifitas(aktifitas);
            return "redirect:/aktifitas";
        }
    }

    @RequestMapping("aktifitas/{id}/delete")
    public String delete(@PathVariable Long id) {
    	aktifitasService.deleteAktifitas(id);
        return "redirect:/aktifitas";
    }

    @RequestMapping("aktifitas/importcsv")
    @SuppressWarnings("unchecked")
    public String importUser(@RequestParam(value="file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        if (file.isEmpty() || !StringUtils.endsWith(file.getOriginalFilename(), "csv")) {
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Please select a csv file to upload");
            return "redirect:/aktifitas";
        }
        byte[] bytes = file.getBytes();
        Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
        List<ImportAktifitas> result = new CsvToBeanBuilder<ImportAktifitas>(reader).withType(ImportAktifitas.class).build().parse();

        Map<String, String> roleMap = new HashMap<String, String>();
        for (ImportAktifitas importAktifitas : result) {
            Aktifitas aktifitas = new Aktifitas();
            aktifitas.setCode(importAktifitas.getCode());
            aktifitas.setCodea(importAktifitas.getCodea());
            aktifitas.setKet(importAktifitas.getKet());
            aktifitasService.saveAktifitas(aktifitas);
        }
        return "redirect:/aktifitas";
    }

    @RequestMapping("aktifitas/import")
    public String mapReapExcelDatatoDB(@RequestParam(value="file") MultipartFile reapExcelDataFile, RedirectAttributes redirectAttributes) throws IOException {
        DataFormatter formatter = new DataFormatter();
        if (StringUtils.endsWith(reapExcelDataFile.getOriginalFilename(), "xls")) {
            HSSFWorkbook workbook = new HSSFWorkbook(reapExcelDataFile.getInputStream());
            HSSFSheet worksheet = workbook.getSheetAt(0);
            for(int i=1;i<worksheet.getPhysicalNumberOfRows() ;i++) {
                HSSFRow row = worksheet.getRow(i);
                Aktifitas aktifitas = aktifitasService.findOneByCode(formatter.formatCellValue(row.getCell(1)));
                if (aktifitas == null){
                    aktifitas = new Aktifitas();
                    aktifitas.setCode(formatter.formatCellValue(row.getCell(1)));
                }
                aktifitas.setCodea(formatter.formatCellValue(row.getCell(2)));
                aktifitas.setKet(formatter.formatCellValue(row.getCell(3)));
                aktifitasService.saveAktifitas(aktifitas);
            }
            workbook.close();
        }
        else if (StringUtils.endsWith(reapExcelDataFile.getOriginalFilename(), "xlsx")) {
            XSSFWorkbook workbook = new XSSFWorkbook(reapExcelDataFile.getInputStream());
            XSSFSheet worksheet = workbook.getSheetAt(0);
            for(int i=1;i<worksheet.getPhysicalNumberOfRows() ;i++) {
                XSSFRow row = worksheet.getRow(i);
                Aktifitas aktifitas = aktifitasService.findOneByCode(formatter.formatCellValue(row.getCell(1)));
                if (aktifitas == null){
                    aktifitas = new Aktifitas();
                    aktifitas.setCode(formatter.formatCellValue(row.getCell(1)));
                }
                aktifitas.setCodea(formatter.formatCellValue(row.getCell(2)));
                aktifitas.setKet(formatter.formatCellValue(row.getCell(3)));
                aktifitasService.saveAktifitas(aktifitas);
            }
        }
        else {
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Please select a excel file to upload");
            return "redirect:/aktifitas";
        }
        return "redirect:/aktifitas";
    }
}