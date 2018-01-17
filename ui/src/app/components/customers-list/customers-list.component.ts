import {Component, Input, OnInit} from '@angular/core';
import {CompanyModel} from '../../models/company.model';
import {Router} from '@angular/router';

@Component({
    selector: 'customers-list',
  templateUrl: './customers-list.component.html',
  styleUrls: ['./customers-list.component.scss']
})
export class CustomersListComponent implements OnInit {

  @Input() companies: CompanyModel[];

  constructor(private router: Router) { }

  ngOnInit() {
  }

    public goToDetail(customer) {
        this.router.navigate(['/customers/detail/' + customer._id]);
    }

}
