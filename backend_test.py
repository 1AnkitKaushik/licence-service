#!/usr/bin/env python3

import requests
import json
import sys
from datetime import datetime, timedelta
import time

class LicenseVaultAPITester:
    def __init__(self, base_url="https://license-vault-7.preview.emergentagent.com"):
        self.base_url = base_url
        self.token = None
        self.tests_run = 0
        self.tests_passed = 0
        self.test_license_key = None

    def log_test(self, name, success, message=""):
        """Log test result"""
        self.tests_run += 1
        if success:
            self.tests_passed += 1
            print(f"✅ PASS: {name}")
        else:
            print(f"❌ FAIL: {name} - {message}")

    def make_request(self, method, endpoint, data=None, headers=None):
        """Make HTTP request with error handling"""
        url = f"{self.base_url}/api/{endpoint}"
        
        if headers is None:
            headers = {'Content-Type': 'application/json'}
            
        if self.token:
            headers['Authorization'] = f'Bearer {self.token}'

        try:
            if method == 'GET':
                response = requests.get(url, headers=headers, timeout=30)
            elif method == 'POST':
                response = requests.post(url, json=data, headers=headers, timeout=30)
            elif method == 'PATCH':
                response = requests.patch(url, json=data, headers=headers, timeout=30)
            elif method == 'DELETE':
                response = requests.delete(url, headers=headers, timeout=30)
            else:
                return None, f"Unsupported method: {method}"

            return response, None
            
        except requests.exceptions.RequestException as e:
            return None, str(e)

    def test_auth_login(self):
        """Test authentication login"""
        print("\n🔐 Testing Authentication...")
        
        response, error = self.make_request('POST', 'auth/login', {
            "username": "admin",
            "password": "admin123"
        })
        
        if error:
            self.log_test("Authentication Login", False, error)
            return False
            
        if response.status_code == 200:
            try:
                data = response.json()
                if 'token' in data:
                    self.token = data['token']
                    self.log_test("Authentication Login", True)
                    return True
                else:
                    self.log_test("Authentication Login", False, "No token in response")
                    return False
            except:
                self.log_test("Authentication Login", False, "Invalid JSON response")
                return False
        else:
            self.log_test("Authentication Login", False, f"HTTP {response.status_code}")
            return False

    def test_license_generation(self):
        """Test license generation with grace period"""
        print("\n📝 Testing License Generation...")
        
        license_data = {
            "username": "test_user",
            "email": "test@example.com",
            "subscriptionType": "YEARLY",
            "maxActivations": 1,
            "gracePeriodDays": 14
        }
        
        response, error = self.make_request('POST', 'license/generate', license_data)
        
        if error:
            self.log_test("License Generation", False, error)
            return False
            
        if response.status_code == 200:
            try:
                license = response.json()
                if 'licenseKey' in license:
                    self.test_license_key = license['licenseKey']
                    
                    # Validate required fields
                    required_fields = ['licenseKey', 'username', 'email', 'subscriptionType', 'gracePeriodDays']
                    missing_fields = [field for field in required_fields if field not in license]
                    
                    if missing_fields:
                        self.log_test("License Generation", False, f"Missing fields: {missing_fields}")
                        return False
                        
                    # Check grace period is set correctly
                    if license['gracePeriodDays'] == 14:
                        self.log_test("License Generation", True)
                        print(f"   Generated license: {self.test_license_key}")
                        return True
                    else:
                        self.log_test("License Generation", False, f"Grace period mismatch: expected 14, got {license.get('gracePeriodDays')}")
                        return False
                else:
                    self.log_test("License Generation", False, "No licenseKey in response")
                    return False
            except:
                self.log_test("License Generation", False, "Invalid JSON response")
                return False
        else:
            self.log_test("License Generation", False, f"HTTP {response.status_code}")
            return False

    def test_license_validation(self):
        """Test license validation and check access levels"""
        print("\n✅ Testing License Validation...")
        
        if not self.test_license_key:
            self.log_test("License Validation - No Key", False, "No test license key available")
            return False
            
        response, error = self.make_request('POST', 'license/validate', {
            "licenseKey": self.test_license_key,
            "machineId": "test-machine-123"
        })
        
        if error:
            self.log_test("License Validation", False, error)
            return False
            
        if response.status_code == 200:
            try:
                validation = response.json()
                required_fields = ['valid', 'accessLevel', 'premiumFeaturesEnabled', 'writeAccessEnabled']
                missing_fields = [field for field in required_fields if field not in validation]
                
                if missing_fields:
                    self.log_test("License Validation", False, f"Missing fields: {missing_fields}")
                    return False
                
                # Check access level is FULL for new license
                if validation.get('accessLevel') == 'FULL' and validation.get('valid') == True:
                    self.log_test("License Validation", True)
                    print(f"   Access Level: {validation['accessLevel']}")
                    print(f"   Premium Features: {validation['premiumFeaturesEnabled']}")
                    print(f"   Write Access: {validation['writeAccessEnabled']}")
                    return True
                else:
                    self.log_test("License Validation", False, f"Expected FULL access, got {validation.get('accessLevel')}")
                    return False
            except:
                self.log_test("License Validation", False, "Invalid JSON response")
                return False
        else:
            self.log_test("License Validation", False, f"HTTP {response.status_code}")
            return False

    def test_license_file_download(self):
        """Test license file download"""
        print("\n📥 Testing License File Download...")
        
        if not self.test_license_key:
            self.log_test("License File Download", False, "No test license key available")
            return ""
            
        response, error = self.make_request('GET', f'license/{self.test_license_key}/download')
        
        if error:
            self.log_test("License File Download", False, error)
            return ""
            
        if response.status_code == 200:
            try:
                data = response.json()
                if 'licenseFile' in data and 'filename' in data:
                    license_file_content = data['licenseFile']
                    
                    # Basic validation of license file format
                    if '.' in license_file_content and len(license_file_content.split('.')) == 2:
                        self.log_test("License File Download", True)
                        print(f"   Filename: {data['filename']}")
                        return license_file_content
                    else:
                        self.log_test("License File Download", False, "Invalid license file format")
                        return ""
                else:
                    self.log_test("License File Download", False, "Missing licenseFile or filename in response")
                    return ""
            except:
                self.log_test("License File Download", False, "Invalid JSON response")
                return ""
        else:
            self.log_test("License File Download", False, f"HTTP {response.status_code}")
            return ""

    def test_offline_license_validation(self, license_file_content):
        """Test offline license file validation"""
        print("\n🔄 Testing Offline License Validation...")
        
        if not license_file_content:
            self.log_test("Offline License Validation", False, "No license file content available")
            return False
            
        response, error = self.make_request('POST', 'license/validate-file', {
            "licenseFile": license_file_content
        })
        
        if error:
            self.log_test("Offline License Validation", False, error)
            return False
            
        if response.status_code == 200:
            try:
                validation = response.json()
                required_fields = ['valid', 'accessLevel']
                missing_fields = [field for field in required_fields if field not in validation]
                
                if missing_fields:
                    self.log_test("Offline License Validation", False, f"Missing fields: {missing_fields}")
                    return False
                
                if validation.get('valid') == True:
                    self.log_test("Offline License Validation", True)
                    print(f"   Access Level: {validation['accessLevel']}")
                    print(f"   Message: {validation.get('message', 'N/A')}")
                    return True
                else:
                    self.log_test("Offline License Validation", False, f"Validation failed: {validation.get('message')}")
                    return False
            except:
                self.log_test("Offline License Validation", False, "Invalid JSON response")
                return False
        else:
            self.log_test("Offline License Validation", False, f"HTTP {response.status_code}")
            return False

    def test_grace_period_update(self):
        """Test grace period configuration update"""
        print("\n⏰ Testing Grace Period Update...")
        
        if not self.test_license_key:
            self.log_test("Grace Period Update", False, "No test license key available")
            return False
            
        response, error = self.make_request('PATCH', f'license/{self.test_license_key}/grace-period', {
            "gracePeriodDays": 7
        })
        
        if error:
            self.log_test("Grace Period Update", False, error)
            return False
            
        if response.status_code == 200:
            try:
                license = response.json()
                if license.get('gracePeriodDays') == 7:
                    self.log_test("Grace Period Update", True)
                    print(f"   Updated grace period to: {license['gracePeriodDays']} days")
                    return True
                else:
                    self.log_test("Grace Period Update", False, f"Grace period not updated correctly: {license.get('gracePeriodDays')}")
                    return False
            except:
                self.log_test("Grace Period Update", False, "Invalid JSON response")
                return False
        else:
            self.log_test("Grace Period Update", False, f"HTTP {response.status_code}")
            return False

    def test_license_stats(self):
        """Test license stats endpoint for grace period count"""
        print("\n📊 Testing License Stats...")
        
        response, error = self.make_request('GET', 'license/stats')
        
        if error:
            self.log_test("License Stats", False, error)
            return False
            
        if response.status_code == 200:
            try:
                stats = response.json()
                required_fields = ['total', 'active', 'expired', 'suspended', 'revoked', 'gracePeriod', 'expiringSoon']
                missing_fields = [field for field in required_fields if field not in stats]
                
                if missing_fields:
                    self.log_test("License Stats", False, f"Missing fields: {missing_fields}")
                    return False
                
                self.log_test("License Stats", True)
                print(f"   Total: {stats['total']}")
                print(f"   Active: {stats['active']}")
                print(f"   Grace Period: {stats['gracePeriod']}")
                print(f"   Expired: {stats['expired']}")
                return True
            except:
                self.log_test("License Stats", False, "Invalid JSON response")
                return False
        else:
            self.log_test("License Stats", False, f"HTTP {response.status_code}")
            return False

    def test_predefined_license(self):
        """Test the predefined test license IZ1U-ERY8-TOMU-EK0U"""
        print("\n🧪 Testing Predefined License...")
        
        test_license = "IZ1U-ERY8-TOMU-EK0U"
        
        response, error = self.make_request('POST', 'license/validate', {
            "licenseKey": test_license,
            "machineId": "test-machine-predefined"
        })
        
        if error:
            self.log_test("Predefined License Validation", False, error)
            return False
            
        if response.status_code == 200:
            try:
                validation = response.json()
                if 'accessLevel' in validation:
                    self.log_test("Predefined License Validation", True)
                    print(f"   License Key: {test_license}")
                    print(f"   Valid: {validation.get('valid', 'N/A')}")
                    print(f"   Access Level: {validation.get('accessLevel', 'N/A')}")
                    print(f"   Status: {validation.get('status', 'N/A')}")
                    
                    # Test downloading license file for predefined license
                    file_response, file_error = self.make_request('GET', f'license/{test_license}/download')
                    if file_error:
                        self.log_test("Predefined License File Download", False, file_error)
                    elif file_response.status_code == 200:
                        self.log_test("Predefined License File Download", True)
                        print(f"   License file downloaded successfully")
                    else:
                        self.log_test("Predefined License File Download", False, f"HTTP {file_response.status_code}")
                    
                    return True
                else:
                    self.log_test("Predefined License Validation", False, "Missing accessLevel in response")
                    return False
            except:
                self.log_test("Predefined License Validation", False, "Invalid JSON response")
                return False
        else:
            self.log_test("Predefined License Validation", False, f"HTTP {response.status_code}")
            return False

    def run_all_tests(self):
        """Run all backend API tests"""
        print("🚀 Starting License Vault Backend API Tests...")
        print(f"📍 Testing against: {self.base_url}")
        
        # Authentication
        if not self.test_auth_login():
            print("\n❌ Authentication failed - cannot continue with authenticated tests")
            return False
        
        # Core license operations
        self.test_license_generation()
        
        license_file = ""
        if self.test_license_key:
            self.test_license_validation()
            license_file = self.test_license_file_download()
            
        if license_file:
            self.test_offline_license_validation(license_file)
            
        if self.test_license_key:
            self.test_grace_period_update()
            
        self.test_license_stats()
        self.test_predefined_license()
        
        # Print summary
        print(f"\n📈 Test Summary:")
        print(f"   Tests Run: {self.tests_run}")
        print(f"   Tests Passed: {self.tests_passed}")
        print(f"   Tests Failed: {self.tests_run - self.tests_passed}")
        print(f"   Success Rate: {(self.tests_passed/self.tests_run*100):.1f}%")
        
        return self.tests_passed == self.tests_run

def main():
    """Main test execution"""
    tester = LicenseVaultAPITester()
    
    success = tester.run_all_tests()
    
    if success:
        print("\n🎉 All tests passed!")
        return 0
    else:
        print("\n💥 Some tests failed!")
        return 1

if __name__ == "__main__":
    sys.exit(main())