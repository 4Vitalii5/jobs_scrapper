package com.example.techstars.model;

import lombok.Getter;

@Getter
public enum Function {
    ADMINISTRATION("Administration"),
    MARKETING_COMMUNICATIONS("Marketing & Communications"),
    SOFTWARE_ENGINEERING("Software Engineering"),
    IT("IT"),
    ACCOUNTING_FINANCE("Accounting & Finance"),
    OTHER_ENGINEERING("Other Engineering"),
    PRODUCT("Product"),
    PEOPLE_HR("People & HR"),
    CUSTOMER_SERVICE("Customer Service"),
    DESIGN("Design"),
    LEGAL("Legal"),
    SALES_BUSINESS_DEVELOPMENT("Sales & Business Development"),
    OPERATIONS("Operations"),
    DATA_SCIENCE("Data Science"),
    QUALITY_ASSURANCE("Quality Assurance"),
    COMPLIANCE_REGULATORY("Compliance / Regulatory");

    private final String label;

    Function(String label) {
        this.label = label;
    }

}
