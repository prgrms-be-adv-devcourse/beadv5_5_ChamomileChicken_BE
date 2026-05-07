# JabaClass (잡아 클래스)

<img width="1492" height="844" alt="image" src="https://github.com/user-attachments/assets/05869bee-4807-4d3f-9d62-06b80167c900" />




<br><br><br>


## 서비스 소개

JabaClass는 원데이 클래스를 더 쉽게 찾고, 더 빠르게 예약할 수 있도록 만든 클래스 플랫폼입니다.

하고 싶은 취미는 많지만,  
어디서 찾아야 할지 모르겠고 비교하는 과정이 번거로워 시작을 미루게 되는 경우가 많습니다.

JabaClass는 이런 과정을 줄여  
사용자가 고민보다 빠르게 클래스를 발견하고, 바로 예약까지 이어질 수 있도록 돕는 것을 목표로 합니다.


### 타겟

- 뭔가 해보고 싶지만 아직 시작하지 못한 사람
- 취미를 찾다가 항상 흐지부지 되는 사람
- 고민보다 빠르게 실행하고 싶은 사람


### 이름에 담은 의미

JabaClass는 `Java Class`이기도 하고,  
동시에 **“잡아 Class”**, 즉 클래스를 하나 잡아서 시작해보자는 의미도 담고 있습니다.


<br><br>



## 주요 기능

### 클래스 탐색

사용자는 다양한 원데이 클래스를 탐색하고 상세 정보를 확인할 수 있습니다.

<br>

### 클래스 예약 및 결제

원하는 일정에 맞춰 클래스를 예약하고 결제를 진행할 수 있습니다.

<br>

### 셀러 상품 관리

셀러는 클래스를 등록하고 수정하며 일정과 운영 정보를 직접 관리할 수 있습니다.

<br>

### 리뷰

사용자는 수강 후 리뷰를 작성하고, 다른 사용자는 이를 참고해 클래스를 선택할 수 있습니다.

<br>


### 운영 기능

주문, 결제, 환불, 정산 흐름이 운영 기능과 연결되어 있습니다.


<br><br>



## 아키텍처 + CI/CD

<img width="1315" height="724" alt="image" src="https://github.com/user-attachments/assets/6b4b8c11-8561-4c11-a634-f0c73d2f13a5" />

<br>

JabaClass는 사용자, 상품, 주문, 결제, 정산 도메인을 분리한 구조로 설계했습니다.  
서비스 간 책임을 분리하고, 이벤트 기반 흐름을 통해 주문 이후의 처리까지 연결되도록 구성했습니다.

<br>

### CI/CD

- GitHub Actions 기반 빌드 및 테스트 자동화
- 컨테이너 기반 배포
- Kubernetes 환경 반영
- Prometheus / Grafana 기반 모니터링


<br><br>



## 개발 문서 (Wiki)

프로젝트를 진행하면서 정리한 기록들입니다.

### Team Culture

- [팀 문화](여기에_링크)
- [컨벤션](여기에_링크)


### Trouble Shooting

- [트러블 슈팅 1](여기에_링크)
- [트러블 슈팅 2](여기에_링크)
- [설계 기록](여기에_링크)


<br><br>



## 기술 스택

### Frontend

![Vue.js](https://img.shields.io/badge/Vue.js-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Pinia](https://img.shields.io/badge/Pinia-FFD859?style=for-the-badge&logo=pinia&logoColor=black)
![Vue Router](https://img.shields.io/badge/Vue_Router-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)


### Backend

![Java](https://img.shields.io/badge/Java-437291?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)


### Infra / DevOps

![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)



<br><br>



## 팀원 소개

| <img src="https://github.com/xub2.png" width="100" height="100"/> | <img src="https://github.com/choi38995.png" width="100" height="100"/> | <img src="https://github.com/JK-LEE98.png" width="100" height="100"/> | <img src="https://github.com/maark1106.png" width="100" height="100"/> | <img src="https://github.com/mirupio.png" width="100" height="100"/> | <img src="https://github.com/reflash407.png" width="100" height="100"/> |
| :---: | :---: | :---: | :---: | :---: | :---: |
| [@xub2](https://github.com/xub2) | [@choi38995](https://github.com/choi38995) | [@JK-LEE98](https://github.com/JK-LEE98) | [@maark1106](https://github.com/maark1106) | [@mirupio](https://github.com/mirupio) | [@reflash407](https://github.com/reflash407) |
| 임요섭 | 조나현 | 이준규 | 황준영 | 박정하 | 이용구 |

<br>

