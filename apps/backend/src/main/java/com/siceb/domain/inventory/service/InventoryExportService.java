package com.siceb.domain.inventory.service;

import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.repository.InventoryItemRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryExportService {

    private static final String[] HEADERS = {
            "SKU", "Name", "Category", "Current Stock", "Min Threshold",
            "Unit", "Stock Status", "Expiration Status", "Expiration Date"
    };

    private final InventoryItemRepository itemRepository;

    public InventoryExportService(InventoryItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public void exportToExcel(UUID branchId, OutputStream outputStream) throws IOException {
        List<InventoryItem> items = itemRepository.findByBranchId(branchId, Pageable.unpaged()).getContent();

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Inventory");
            writeHeader(sheet);
            writeRows(sheet, items);
            workbook.write(outputStream);
        }
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
    }

    private void writeRows(Sheet sheet, List<InventoryItem> items) {
        int rowNum = 1;
        for (InventoryItem item : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getSku());
            row.createCell(1).setCellValue(item.getName());
            row.createCell(2).setCellValue(item.getCategory());
            row.createCell(3).setCellValue(item.getCurrentStock());
            row.createCell(4).setCellValue(item.getMinThreshold());
            row.createCell(5).setCellValue(item.getUnitOfMeasure());
            row.createCell(6).setCellValue(item.getStockStatus().name());
            row.createCell(7).setCellValue(item.getExpirationStatus().name());
            row.createCell(8).setCellValue(
                    item.getExpirationDate() != null ? item.getExpirationDate().toString() : "");
        }
    }
}
